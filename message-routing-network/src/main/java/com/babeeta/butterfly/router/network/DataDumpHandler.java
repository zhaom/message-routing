package com.babeeta.butterfly.router.network;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;

@Sharable
public class DataDumpHandler implements ChannelUpstreamHandler {

	private DataOutputStream out = null;

	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
			throws Exception {
		if (out != null && e instanceof MessageEvent) {
			MessageEvent me = (MessageEvent) e;
			ChannelBuffer buf = (ChannelBuffer) me.getMessage();
			buf.markReaderIndex();

			synchronized (out) {
				out.writeInt(e.getChannel().getId());
				out.writeInt(buf.readableBytes());
				buf.readBytes(out, buf.readableBytes());
				out.flush();
			}
		}
		ctx.sendUpstream(e);
	}

	public void startDump(String file) throws IOException {
		if (out != null) {
			out.close();
		}
		OutputStream fileOut = new BufferedOutputStream(new FileOutputStream(
				file));
		out = new DataOutputStream(fileOut);
	}

	public void stopDump() throws IOException {
		if (this.out != null) {
			out.close();
			out = null;
		}
	}
}