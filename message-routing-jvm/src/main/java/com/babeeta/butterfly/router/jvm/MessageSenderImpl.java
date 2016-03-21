package com.babeeta.butterfly.router.jvm;

import java.net.UnknownServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.babeeta.butterfly.MessageFuture;
import com.babeeta.butterfly.MessageRouting.Message;
import com.babeeta.butterfly.MessageSender;

/**
 * 消息发送的JVM实现
 * 
 * @author leon
 * 
 */
public class MessageSenderImpl implements MessageSender {

	private static final Logger logger = LoggerFactory
			.getLogger(MessageSenderImpl.class);

	private final DNS dns;

	public MessageSenderImpl(DNS dns) {
		super();
		this.dns = dns;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.babeeta.butterfly.MessageSender#send(com.babeeta.butterfly.MessageRouting
	 * .Message)
	 */
	@Override
	public MessageFuture send(Message message) {
		logger.debug("[{}] to {}", message.getUid(), message.getTo());
		MessageFutureImpl messageFutureImpl = new MessageFutureImpl();
		if (message == null || message.getTo() == null
				|| message.getTo().trim().indexOf("@") == -1) {
			logger.debug("[{}] illegalArgument. Message has been dropped.",
					message.getUid());
			messageFutureImpl.failed(new IllegalArgumentException(
					"Illegal property: to"));
			return messageFutureImpl;
		}

		String domain = getDomain(message.getTo());
		MessageService service = dns.resolve(domain);
		if (service == null) {
			logger.error(
					"[{}] UnknownService: [{}]. Message has been dropped.",
					message.getUid(), domain);
			messageFutureImpl.failed(new UnknownServiceException(domain));
		} else {
			logger.debug("[{}] committing.", message.getUid());
			service.commit(message);
			messageFutureImpl.success();
		}
		return messageFutureImpl;
	}

	private String getDomain(String addr) {
		return addr.substring(addr.indexOf("@") + 1, addr.length());
	}

}
