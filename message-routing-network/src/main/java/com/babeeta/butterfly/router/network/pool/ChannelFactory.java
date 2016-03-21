package com.babeeta.butterfly.router.network.pool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeoutException;

import org.jboss.netty.channel.Channel;

/**
 * Channel的Factory。负责创建到目的地的网络链接
 * 
 * @author leon
 * 
 */
public interface ChannelFactory {
	Channel getChannel(InetSocketAddress inetSocketAddress)
			throws TimeoutException, IOException;

	void returnChannel(Channel channel);

	void shutdown() throws Exception;
}
