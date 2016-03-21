package com.babeeta.butterfly;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.babeeta.butterfly.MessageRouting.Message;

public abstract class AbstractMessageRouter implements MessageHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(AbstractMessageRouter.class);

	private final MessageSender messageSender;

	public AbstractMessageRouter(MessageSender messageSender) {
		super();
		this.messageSender = messageSender;
	}

	@Override
	public void onMessage(final Message message) {
		Message transformed = null;
		try {
			transformed = transform(message);
		} catch (Throwable t) {
			logger.error("[{}] is dropped. ", message.getUid(),
					t);
			return;
		}

		if (transformed == null) {
			logger.debug("[{}] is dropped.", message.getUid());
		} else {
			logger.debug("Sending [{}] to [{}]", message.getUid(),
					transformed.getTo());
			messageSender.send(transformed).addListener(
					new MessageFutureListener() {

						@Override
						public void operationComplete(MessageFuture future) {
							if (future.isSuccess()) {
								logger.debug("[{}] is delivered",
										message.getUid());
							} else {
								logger.error("[{}] Cause:{}", message.getUid(),
										future.getCause().getMessage());
							}
						}
					});
		}
	}

	protected MessageSender getMessageSender() {
		return messageSender;
	}

	protected abstract Message transform(Message message);

}
