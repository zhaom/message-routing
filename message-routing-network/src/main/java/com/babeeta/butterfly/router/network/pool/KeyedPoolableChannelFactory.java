package com.babeeta.butterfly.router.network.pool;

import java.net.InetSocketAddress;
import java.rmi.ConnectException;
import java.util.concurrent.ExecutorService;

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.DefaultChannelPipeline;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.babeeta.butterfly.MessageRouting.Message;
import com.google.protobuf.ByteString;

/**
 * Channel的链接池Factory实现， 用于PooledChannelFactory中
 * 
 * @author leon
 * 
 */
final class KeyedPoolableChannelFactory implements KeyedPoolableObjectFactory {
	static class ClientPipelineFactory implements ChannelPipelineFactory {
		private static final DummyHandler DUMMY_HANDLER = new DummyHandler();
		private static final ProtobufEncoder PROTOBUF_ENCODER = new ProtobufEncoder();
		private static final ProtobufVarint32LengthFieldPrepender LENGTH_FIELD_PREPENDER = new ProtobufVarint32LengthFieldPrepender();
		final ChannelPipeline pipeLine = new DefaultChannelPipeline();

		private ClientPipelineFactory() {
		}

		@Override
		public ChannelPipeline getPipeline() throws Exception {
			ChannelPipeline pipeLine = new DefaultChannelPipeline();
			pipeLine.addLast("frameEncoder", LENGTH_FIELD_PREPENDER);
			pipeLine.addLast("protobufEncoder", PROTOBUF_ENCODER);
			pipeLine.addLast("dummy", DUMMY_HANDLER);
			return pipeLine;
		}

	}

	@Sharable
	static class DummyHandler implements ChannelUpstreamHandler {
		@Override
		public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
				throws Exception {
			if (e instanceof ExceptionEvent) {
				logger.error("Error occured:", ((ExceptionEvent) e).getCause());
				e.getChannel().close();
			}
		}

	}

	public static final Message MSG_HEARTBEAT = Message.newBuilder()
			.setUid("heartbeat")
			.setDate(-1)
			.setFrom("")
			.setTo("")
			.setContent(ByteString.EMPTY)
			.build();

	private static final Logger logger = LoggerFactory
			.getLogger(KeyedPoolableChannelFactory.class);

	private final ExecutorService bossExecutorService;
	private final ExecutorService workerExecutorService;
	private final NioClientSocketChannelFactory nioClientSocketChannelFactory;
	private final ClientPipelineFactory clientPipelineFactory = new ClientPipelineFactory();

	public KeyedPoolableChannelFactory(ExecutorService bossExecutorService,
			ExecutorService workerExecutorService) {
		super();
		this.bossExecutorService = bossExecutorService;
		this.workerExecutorService = workerExecutorService;
		this.nioClientSocketChannelFactory = new NioClientSocketChannelFactory(
				this.bossExecutorService, this.workerExecutorService);

	}

	@Override
	public void activateObject(Object key, Object obj) throws Exception {
	}

	@Override
	public void destroyObject(Object key, Object obj) throws Exception {
		Channel channel = (Channel) obj;
		if (obj != null && channel.isConnected()) {
			channel.write(ChannelBuffers.EMPTY_BUFFER).addListener(
					ChannelFutureListener.CLOSE);
		}
	}

	@Override
	public Object makeObject(Object key) throws Exception {
		logger.debug("Making new connection for {}", key);
		ChannelFuture channelFuture = getClientBootstrap().connect(
				((InetSocketAddress) key)).await();
		if (channelFuture.isSuccess()) {
			logger.debug("New connection for {} is connected. ID:{}", key,
					channelFuture.getChannel().getId());
			return channelFuture.getChannel();
		} else if (channelFuture.getCause() != null) {
			logger.error("Ops, something wrong...", channelFuture.getCause());
			if (channelFuture instanceof Exception) {
				throw (Exception) channelFuture.getCause();
			} else {
				throw new RuntimeException(channelFuture.getCause());
			}
		} else {
			throw new ConnectException("Failed to connect to " + key.toString());
		}
	}

	@Override
	public void passivateObject(Object key, Object obj) throws Exception {
	}

	public void shutdown() {
	}

	@Override
	public boolean validateObject(Object key, Object obj) {
		logger.debug("[{}]Validating channel.", obj);
		Channel channel = (Channel) obj;
		if (!channel.isConnected()) {
			logger.debug("[{}]is closed.", ((Channel) obj).getId());
			return false;
		}

		try {
			ChannelFuture future = channel.write(MSG_HEARTBEAT);
			future.await(5000);
			logger.debug("[{}]Write result: {}", channel.getId(),
					future.isSuccess());
			return future.isSuccess();
		} catch (InterruptedException e) {
			return false;
		}
	}

	private ClientBootstrap getClientBootstrap() {
		ClientBootstrap clientBootstrap = new ClientBootstrap();
		clientBootstrap.setFactory(nioClientSocketChannelFactory);
		clientBootstrap.setPipelineFactory(clientPipelineFactory);
		clientBootstrap.setOption("keepAlive", true);
		clientBootstrap.setOption("tcpNoDelay", true);
		return clientBootstrap;
	}
}