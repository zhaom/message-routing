package com.babeeta.butterfly.router.network.pool;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.rmi.ConnectException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.ObjectName;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * ChannelFactory的链接池实现
 * </p>
 * <p>
 * 所有借出的Channel都被加了代理以遍将close操作拦截为入池操作
 * </p>
 * 
 * @author leon
 */
public class PooledChannelFactory extends GenericKeyedObjectPool implements
		ChannelFactory, PooledChannelFactoryMBean {

	private static final Logger logger = LoggerFactory
			.getLogger(PooledChannelFactory.class);

	private static final int DEFAULT_MAX_CONNECTION = 32;
	private static final AtomicInteger counter = new AtomicInteger();

	public final ChannelFutureListener CHANNEL_CLOSE_LISTENER = new ChannelFutureListener() {

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			Channel channel = future.getChannel();
			logger.warn("[{}]Channel closed outside pool.", channel.getId());
			PooledChannelFactory.this.invalidateObject(
					channel.getRemoteAddress(), channel);
			channel.getCloseFuture().removeListener(this);
		}
	};

	public PooledChannelFactory(ExecutorService bossExecutorService,
								ExecutorService workerExecutorService) {
		super(new KeyedPoolableChannelFactory(bossExecutorService,
				workerExecutorService));
		this.setTestOnBorrow(true);
		this.setTestOnReturn(false);
		this.setMaxTotal(DEFAULT_MAX_CONNECTION);
		this.setMaxActive(DEFAULT_MAX_CONNECTION);
		this.setTimeBetweenEvictionRunsMillis(10000L);
		this.setTestWhileIdle(true);

		try {
			ManagementFactory
					.getPlatformMBeanServer()
					.registerMBean(
							this,
							new ObjectName(
									"com.babeeta.butterfly.router.network.pool:name=PooledChannelFactory"
											+ counter.incrementAndGet()));

		} catch (Exception ex) {
			logger.error("Error ocurred while registering mbean.", ex);
		}
	}

	@Override
	public Object borrowObject(final Object key) throws Exception {
		logger.debug("Get channel by [{}].", key.toString());
		final Object result = super.borrowObject(key);
		Channel channel = (Channel) result;
		logger.debug("[{}]BorrowObject", channel.getId());
		if (result == null) {
			return null;
		} else {
			channel.getCloseFuture().addListener(
					CHANNEL_CLOSE_LISTENER);
			return result;
		}
	}

	@Override
	public Channel getChannel(InetSocketAddress inetSocketAddress)
			throws TimeoutException, IOException {
		try {
			return (Channel) this.borrowObject(inetSocketAddress);
		} catch (Exception e) {
			throw new ConnectException("Failed to obtain channel to "
					+ inetSocketAddress.toString(), e);
		}
	}

	@Override
	public int getNumActive(String host) {
		return this.getNumActive(new InetSocketAddress(host, 5757));
	}

	@Override
	public void returnChannel(Channel channel) {
		logger.debug("[{}]Returning channel back to pool.", channel.getId());
		try {
			channel.getCloseFuture().removeListener(CHANNEL_CLOSE_LISTENER);
			this.returnObject(channel.getRemoteAddress(), channel);
		} catch (Exception e) {
			logger.error("Error while returning channel back to pool.", e);
			if (channel != null && channel.isConnected()) {
				channel.close();
			}
		}
	}

	@Override
	public void shutdown() throws Exception {
		close();
	}

}
