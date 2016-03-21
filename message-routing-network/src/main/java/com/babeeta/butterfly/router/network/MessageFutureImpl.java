package com.babeeta.butterfly.router.network;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.babeeta.butterfly.MessageFuture;
import com.babeeta.butterfly.MessageFutureListener;

class MessageFutureImpl implements MessageFuture {

	private static final Logger logger = LoggerFactory
			.getLogger(MessageFutureImpl.class);

	private final Set<MessageFutureListener> listenerSet = Collections
			.synchronizedSet(new HashSet<MessageFutureListener>());
	private ChannelFuture channelFuture = null;
	private Throwable cause = null;

	@Override
	public MessageFuture addListener(MessageFutureListener listener) {
		listenerSet.add(listener);
		if (isDone()) {
			listener.operationComplete(this);
		}
		return this;
	}

	@Override
	public MessageFuture await() throws InterruptedException {
		if (channelFuture != null) {
			channelFuture.await();
		}
		return this;
	}

	@Override
	public boolean await(long timeoutMillis) throws InterruptedException {
		if (channelFuture != null) {
			return channelFuture.await(timeoutMillis);
		}
		return false;
	}

	@Override
	public Throwable getCause() {
		if (this.cause != null) {
			return cause;
		} else {
			if (channelFuture == null) {
				return null;
			} else {
				return channelFuture.getCause();
			}
		}
	}

	@Override
	public boolean isDone() {
		if (channelFuture == null) {
			return getCause() != null;
		} else {
			return channelFuture.isDone();
		}
	}

	@Override
	public boolean isSuccess() {
		if (channelFuture != null) {
			return channelFuture.isSuccess();
		} else {
			return false;
		}
	}

	@Override
	public boolean removeListener(MessageFutureListener listener) {
		return listenerSet.remove(listener);
	}

	MessageFutureImpl setCause(Throwable cause) {
		this.cause = cause;
		if (cause != null) {
			notify(this.channelFuture);
		}
		return this;
	}

	MessageFutureImpl setChannelFuture(ChannelFuture channelFuture) {
		this.channelFuture = channelFuture;
		if (this.channelFuture.isDone()) {
			notify(channelFuture);
		} else {
			this.channelFuture.addListener(new ChannelFutureListener() {

				@Override
				public void operationComplete(ChannelFuture future)
						throws Exception {
					MessageFutureImpl.this.notify(future);
				}
			});
		}
		return this;
	}

	private void notify(ChannelFuture future) {
		for (MessageFutureListener listener : listenerSet) {
			try {
				listener.operationComplete(this);
			} catch (Throwable t) {
				// 单元测试的异常还是要丢出去的。
				if (t instanceof AssertionError) {
					throw (AssertionError) t;
				} else {
					logger.warn("Listener error: {}", t.getMessage());
				}
			}
		}
	}

}