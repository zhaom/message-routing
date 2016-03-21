package com.babeeta.butterfly.router.network;

import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.babeeta.butterfly.MessageHandler;
import com.babeeta.butterfly.MessageRouting.Message;

@Sharable
public class RequestHandler extends SimpleChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(RequestHandler.class);

	public static int getChannelGroupSize() {
		return channelGroup.size();
	}

	private final MessageHandler messageHandler;

	private static ChannelGroup channelGroup;

	public RequestHandler(MessageHandler messageHandler) {
		super();
		this.messageHandler = messageHandler;
		channelGroup = new DefaultChannelGroup();
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		super.channelConnected(ctx, e);
		channelGroup.add(e.getChannel());
	}

	public ChannelGroup getDefaultChannelGroup() {
		return channelGroup;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		if (!(e.getMessage() instanceof Message)) {
			logger.error("Unsupported message type: {}", e.getMessage()
					.getClass());
		} else if ("heartbeat".equals(((Message) e.getMessage()).getUid())) {
			logger.info("[{}]Heartbeat from {}", e.getChannel().getId(), e
					.getChannel().getRemoteAddress());
		} else {
			messageHandler.onMessage((Message) e.getMessage());
		}
	}
}