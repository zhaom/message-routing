package com.babeeta.butterfly.rpc;

import java.lang.management.ManagementFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.babeeta.butterfly.MessageFuture;
import com.babeeta.butterfly.MessageFutureListener;
import com.babeeta.butterfly.MessageHandler;
import com.babeeta.butterfly.MessageRouting.Message;
import com.babeeta.butterfly.MessageSender;

public class MessageBasedRPCService implements MessageHandler, RPCService,
		MessageBasedRPCServiceMBean {

	private final static Logger logger = LoggerFactory
			.getLogger(MessageBasedRPCService.class);
	private final ConcurrentMap<String, RPCInvoke> rpcRegistry = new ConcurrentHashMap<String, RPCInvoke>();
	private final MessageSender messageSender;
	private final ScheduledExecutorService timeoutCleaner = Executors
			.newScheduledThreadPool(
					(Runtime.getRuntime().availableProcessors() < 8 ? 8
							: Runtime.getRuntime().availableProcessors()) + 1,
					new DaemonThreadFactory());
	// 默认的调用超时为1分钟
	long invokeTimeout = 60;

	private final AtomicLong invokeCounter = new AtomicLong();

	public MessageBasedRPCService(MessageSender messageSender) {
		this.messageSender = messageSender;
		try {
			ObjectName objectName = new ObjectName(
					"com.babeeta.butterfly.rpc:name=MessageBasedRPCService");
			ManagementFactory.getPlatformMBeanServer().registerMBean(this,
					objectName);
		} catch (Exception ex) {
			logger.error("Error ocurred while setting up Mbean", ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.babeeta.butterfly.rpc.MessageBasedRPCServiceMBean#getCounter()
	 */
	@Override
	public long getCounter() {
		return invokeCounter.getAndSet(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.babeeta.butterfly.rpc.MessageBasedRPCServiceMBean#getPendingInvokeCount
	 * ()
	 */
	@Override
	public int getPendingInvokeCount() {
		return rpcRegistry.size();
	}

	@Override
	public void invoke(final Message message, final RPCHandler responseHandler) {
		logger.debug("New invoke: {}", message.getUid());
		invokeCounter.incrementAndGet();
		// 注册调用动作，启动超时计时器
		final ScheduledFuture<?> scheduledFuture = timeoutCleaner.schedule(
				new Runnable() {

					@Override
					public void run() {
						RPCInvoke rpcInvoke = rpcRegistry.remove(message
								.getUid());
						if (rpcInvoke != null) {
							rpcInvoke.handler.exceptionCaught(message,
									new TimeoutException());
						}
					}

				}, invokeTimeout, TimeUnit.SECONDS);

		rpcRegistry.put(message.getUid(), new RPCInvoke(responseHandler,
				scheduledFuture));

		// 发送消息
		messageSender.send(message).addListener(new MessageFutureListener() {

			@Override
			public void operationComplete(MessageFuture future) {
				if (future.isSuccess()) {
					logger.debug("[{}]Message has been sent to service.",
							message.getUid());
				} else {
					logger.debug("[{}]Failed: {}", message.getUid(), future
							.getCause().toString());
					try {
						scheduledFuture.cancel(false);
						responseHandler.exceptionCaught(message,
								future.getCause());
					} catch (Throwable t) {
						logger.error(
								"[{}]Error occured while invoking execptionCaught: {}",
								message.getUid(), t.toString());
					}
				}
			}
		});
	}

	@Override
	public void onMessage(Message message) {
		RPCInvoke rpcInvoke = rpcRegistry.remove(message.getReplyFor());
		if (rpcInvoke == null) {
			logger.error("[{}]No suitable handler.Message has been dropped.",
					message.getReplyFor());
		} else {
			logger.debug("[{}]Got response.", message.getReplyFor());
			boolean canceled = rpcInvoke.scheduledFuture.cancel(false);
			logger.debug("[{}]RPCInvoke timer has been canceled. {}", canceled);
			try {
				rpcInvoke.handler.onMessage(message);
			} catch (Throwable t) {
				logger.error("[{}]Error occured while invoking onMessage: {}",
						message.getReplyFor(), t.toString());
			}
		}
	}
}

class RPCInvoke {
	final RPCHandler handler;
	final ScheduledFuture<?> scheduledFuture;

	public RPCInvoke(RPCHandler handler, ScheduledFuture<?> scheduledFuture) {
		super();
		this.handler = handler;
		this.scheduledFuture = scheduledFuture;
	}

}
