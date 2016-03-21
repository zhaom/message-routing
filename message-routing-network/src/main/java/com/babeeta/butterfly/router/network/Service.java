package com.babeeta.butterfly.router.network;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.DefaultChannelPipeline;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.babeeta.butterfly.MessageHandler;
import com.babeeta.butterfly.MessageRouting;
import com.babeeta.butterfly.router.network.dns.DNSClient;
import com.babeeta.butterfly.router.network.dns.DNSClientDefaultImpl;
import com.babeeta.butterfly.router.network.monitor.Network;

/**
 * <p>
 * 路由器的网络通讯层Server实现
 * </p>
 * <p>
 * 默认使用SRV记录来启动服务
 * </p>
 * 
 * @author leon
 */
public class Service {
	private static final Logger logger = LoggerFactory.getLogger(Service.class);
	public static final String DEFAULT_BIND_ADDRESS = "0.0.0.0";
	private final int DEFAULT_PORT = 5757;
	private final ExecutorService workerExecutorService;
	private final ExecutorService bossExecutorService;
	private final DNSClient dnsClient;
	private NioServerSocketChannelFactory nioServerSocketChannelFactory;
	private Channel bossChannel;

	private final RequestHandler requestHandler;
	private final DataDumpHandler dataDumpHandler;

	public Service(MessageHandler messageHandler,
			ExecutorService workerExecutorService,
			ExecutorService bossExecutorService) {
		this(new DNSClientDefaultImpl(), messageHandler, workerExecutorService,
				bossExecutorService);
	}

	Service(DNSClient dnsClient, MessageHandler messageHandler,
					ExecutorService workerExecutorService,
					ExecutorService bossExecutorService) {
		this.dnsClient = dnsClient;
		this.workerExecutorService = workerExecutorService;
		this.bossExecutorService = bossExecutorService;
		this.dataDumpHandler = new DataDumpHandler();
		this.requestHandler = new RequestHandler(messageHandler);
	}

	/**
	 * 关闭Server
	 * 
	 * @throws InterruptedException
	 */
	public void shutdownGraceFully() {
		logger.info("Shutting down...");
		bossChannel.close().awaitUninterruptibly();
		requestHandler.getDefaultChannelGroup().close()
				.awaitUninterruptibly();
		logger.info("See you next time.");
	}

	/**
	 * 使用参数启动Server
	 * 
	 * @param bindAddress
	 * @param port
	 */
	public void start(String serviceName) throws Exception {
		logger.info("bootstrapping...");
		nioServerSocketChannelFactory = new NioServerSocketChannelFactory(
				bossExecutorService, workerExecutorService);

		ServerBootstrap serverBootstrap = new ServerBootstrap(
				nioServerSocketChannelFactory);
		serverBootstrap.setOption("keepAlive", true);
		serverBootstrap.setOption("tcpNoDelay", true);
		serverBootstrap.setPipelineFactory(new NetworkServicePipelineFactory(
				requestHandler, dataDumpHandler));

		try {
			bossChannel = serverBootstrap.bind(new InetSocketAddress(
					DEFAULT_BIND_ADDRESS, getPort(serviceName)));
		} catch (UnknownHostException e) {
			logger.error("Failed to bind.", e);
			throw e;
		}

		logger.info("Router is serving on {}", bossChannel.getLocalAddress());

		new Network(dataDumpHandler, bossExecutorService);
	}

	private int getPort(String serviceName) throws UnknownHostException {
		logger.info("Resoving service name.");
		InetSocketAddress result = dnsClient.resove(serviceName);
		logger.info("Service port: {}", result.getPort());
		return result.getPort();
	}
}

class NetworkServicePipelineFactory implements ChannelPipelineFactory {

	private final RequestHandler requestHandler;
	private final DataDumpHandler dataDumpHandler;
	private final ProtobufDecoder protobufDecoder = new ProtobufDecoder(
			MessageRouting.Message.getDefaultInstance());

	public NetworkServicePipelineFactory(RequestHandler requestHandler,
											DataDumpHandler dataDumpHandler) {
		super();
		this.requestHandler = requestHandler;
		this.dataDumpHandler = dataDumpHandler;
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = new DefaultChannelPipeline();
		pipeline.addLast("dump", dataDumpHandler);

		pipeline.addLast("frameDecoder",
				new ProtobufVarint32FrameDecoder());
		pipeline
				.addLast(
						"protobuf", protobufDecoder);

		pipeline.addLast("request handler", requestHandler);
		return pipeline;
	}
}