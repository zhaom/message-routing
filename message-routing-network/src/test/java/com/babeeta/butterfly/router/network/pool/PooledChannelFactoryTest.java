package com.babeeta.butterfly.router.network.pool;

import static org.junit.Assert.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import org.jboss.netty.bootstrap.*;
import org.jboss.netty.buffer.*;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.group.*;
import org.jboss.netty.channel.socket.nio.*;
import org.junit.*;

public class PooledChannelFactoryTest {

	@Sharable
	private final class DummyHandler implements ChannelUpstreamHandler {
		@Override
		public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
				throws Exception {
			if (e instanceof ChannelStateEvent
					&& ChannelState.CONNECTED == ((ChannelStateEvent) e)
							.getState()) {
				channelGroup.add(e.getChannel());
			}
		}
	}

	private static final int MAX_CONNECTION = 32;

	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final ExecutorService mockExecutors = Executors
			.newCachedThreadPool();
	private final PooledChannelFactory pooledChannelFactory = new PooledChannelFactory(
			executor, executor);
	private final ChannelGroup channelGroup = new DefaultChannelGroup();

	private final NioServerSocketChannelFactory nioServerSocketChannelFactory = new NioServerSocketChannelFactory(
			mockExecutors, mockExecutors, (Runtime.getRuntime()
					.availableProcessors() < 8 ? 8 : Runtime.getRuntime()
					.availableProcessors()));

	@Before
	public void setUp() throws Exception {
		ServerBootstrap bootstrap = new ServerBootstrap(
				nioServerSocketChannelFactory);
		bootstrap.getPipeline().addLast("handler", new DummyHandler());

		channelGroup.add(bootstrap.bind(new InetSocketAddress("localhost",
				15757)));

	}

	@After
	public void tearDown() throws Exception {
		channelGroup.close().awaitUninterruptibly();
		nioServerSocketChannelFactory.releaseExternalResources();
		pooledChannelFactory.shutdown();
	}

	@Test
	public void testGetChannel() throws TimeoutException, IOException,
			InterruptedException {
		assertEquals(MAX_CONNECTION, pooledChannelFactory.getMaxTotal());
		assertEquals(MAX_CONNECTION, pooledChannelFactory.getMaxActive());

		final ChannelBuffer buf = ChannelBuffers.wrappedBuffer("Hello, there"
				.getBytes());
		assertEquals(0, pooledChannelFactory.getNumActive());

		List<Channel> channelList = new ArrayList<Channel>();

		for (int i = 0; i < 32; i++) {
			channelList.add(pooledChannelFactory
					.getChannel(new InetSocketAddress("localhost", 15757)));
		}

		assertEquals(MAX_CONNECTION, pooledChannelFactory.getNumActive());

		for (Channel channel : channelList) {
			channel.write(buf);
		}
		assertEquals(MAX_CONNECTION, pooledChannelFactory.getNumActive());

		for (Channel channel : channelList) {
			pooledChannelFactory.returnChannel(channel);
		}

		Thread.sleep(100L);

		assertEquals(0, pooledChannelFactory.getNumActive());
	}

	@Test
	public void testOnBorrow() throws Exception {
		final Channel channel = (Channel) pooledChannelFactory
				.borrowObject(new InetSocketAddress("localhost", 15757));
		pooledChannelFactory.returnChannel(channel);

		assertEquals(true, pooledChannelFactory.getTestOnBorrow());
		assertEquals(false, pooledChannelFactory.getTestOnReturn());
		assertEquals(1, pooledChannelFactory.getNumIdle());

		Channel obj = (Channel) pooledChannelFactory
				.borrowObject(new InetSocketAddress("localhost", 15757));

		assertSame(channel, obj);

		pooledChannelFactory.returnChannel(obj);

	}
}