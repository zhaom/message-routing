package com.babeeta.butterfly.router.network;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.babeeta.butterfly.MessageFuture;
import com.babeeta.butterfly.MessageRouting.Message;
import com.babeeta.butterfly.router.network.dns.DNSClient;
import com.babeeta.butterfly.router.network.dns.DNSClientDefaultImpl;
import com.babeeta.butterfly.router.network.pool.ChannelFactory;
import com.babeeta.butterfly.router.network.pool.PooledChannelFactory;

/**
 * 消息发送的网络实现
 * 
 * @author leon
 */
public class MessageSenderImpl implements com.babeeta.butterfly.MessageSender,
		MessageSenderImplMBean {

	private static final Logger logger = LoggerFactory
			.getLogger(MessageSenderImpl.class);

	private final ChannelFactory channelFactory;

	private static ThreadPoolExecutor senderExecutorService;
	private static ThreadPoolExecutor channelFactoryExecutorService;

	private final Collection<Runnable> messageQueue;
	private final DNSClient dnsClient;

	public MessageSenderImpl() {
		this(new DNSClientDefaultImpl());
	}

	MessageSenderImpl(DNSClient dnsClient) {
		super();
		this.dnsClient = dnsClient;
		LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
		senderExecutorService = new ThreadPoolExecutor(32, 32, 10,
				TimeUnit.MINUTES,
				queue);
		channelFactoryExecutorService = new ThreadPoolExecutor(32, 32, 10,
				TimeUnit.MINUTES,
				queue);

		messageQueue = Collections.unmodifiableCollection(queue);
		channelFactory = new PooledChannelFactory(
				channelFactoryExecutorService,
				channelFactoryExecutorService);

		try {
			for (int i = 0; i < 100; i++) {
				ManagementFactory
						.getPlatformMBeanServer()
						.registerMBean(
								this,
								new ObjectName(
										"MessageRouting:type=network,name=MessageSender+"
												+ i));
			}
		} catch (Exception e) {
			logger.error("Error register mbean:{}", e.getMessage(), e);
		}
	}

	@Override
	public int getMessageQueueLength() {
		return messageQueue.size();
	}

	@Override
	public int getThreadPoolSize() {
		return senderExecutorService.getCorePoolSize();
	}

	@Override
	public MessageFuture send(final Message message) {
		final MessageFutureImpl future = new MessageFutureImpl();
		senderExecutorService.execute(new Runnable() {
			@Override
			public void run() {
				String domain = message.getTo().substring(
						message.getTo().indexOf("@") + 1,
						message.getTo().length());
				Channel channel = null;
				try {
					InetSocketAddress address = new InetSocketAddress(domain,
							dnsClient.resove(domain).getPort());
					channel = channelFactory.getChannel(address);
					ChannelFuture channelFuture = channel.write(message);
					future.setChannelFuture(channelFuture);
					channelFuture.addListener(new ChannelFutureListener() {

						@Override
						public void operationComplete(ChannelFuture future)
								throws Exception {
							if (future.isSuccess()) {
								logger.debug("[{}] sent succeed.",
										message.getUid());
							} else {
								logger.debug("[{}] sent fail.",
										message.getUid());
							}
						}
					});
				} catch (Exception e) {
					logger.error("{}", e.getMessage());
					future.setCause(e);
				} finally {
					logger.debug("Channel check {}", channel);
					if (channel != null) {
						channelFactory.returnChannel(channel);
					}
				}
			}
		});

		return future;
	}

	@Override
	public void setThreadPoolSize(int corePoolSize) {
		senderExecutorService.setCorePoolSize(corePoolSize);
	}
}