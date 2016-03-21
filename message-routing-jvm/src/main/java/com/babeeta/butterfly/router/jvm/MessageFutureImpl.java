package com.babeeta.butterfly.router.jvm;

import com.babeeta.butterfly.MessageFuture;
import com.babeeta.butterfly.MessageFutureListener;

public class MessageFutureImpl implements MessageFuture {

	private Throwable cause;
	private boolean done;

	@Override
	public MessageFuture addListener(MessageFutureListener listener) {
		listener.operationComplete(this);
		return this;
	}

	@Override
	public MessageFuture await() throws InterruptedException {
		return this;
	}

	@Override
	public boolean await(long timeoutMillins) throws InterruptedException {
		return true;
	}

	@Override
	public Throwable getCause() {
		return this.cause;
	}

	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	public boolean isSuccess() {
		return done && (cause == null);
	}

	@Override
	public boolean removeListener(MessageFutureListener listener) {
		return true;
	}

	void failed(Throwable cause) {
		this.done = true;
		this.cause = cause;
	}

	void success() {
		this.done = true;
	}

}
