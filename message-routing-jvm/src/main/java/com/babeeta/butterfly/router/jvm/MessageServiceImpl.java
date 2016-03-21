package com.babeeta.butterfly.router.jvm;

import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.babeeta.butterfly.MessageHandler;
import com.babeeta.butterfly.MessageRouting.Message;

public class MessageServiceImpl implements MessageService {
	private static final Logger logger = LoggerFactory.getLogger(Server.class);

	private final ExecutorService executorService;
	private final MessageHandler messageHandler;

	/**
	 * 创建新的服务并绑定到域名
	 * 
	 * @param domain
	 *            要绑定到的域名
	 * @param messageHandler
	 *            处理消息的MessageHandler
	 * @param executorService
	 *            用于执行MessageHandler
	 * @throws IllegalArgumentException
	 *             域名为空时会抛出
	 * @throws DomainAlreadyRegisteredException
	 *             域名已经被注册了
	 */
	public MessageServiceImpl(MessageHandler messageHandler,
			ExecutorService executorService) throws IllegalArgumentException,
			DomainAlreadyRegisteredException {
		super();
		this.messageHandler = messageHandler;
		this.executorService = executorService;
	}

	/**
	 * 提交消息给服务
	 * 
	 * @param message
	 *            要提交的消息。一般由MessageSender调用
	 */
	@Override
	public void commit(final Message message) {
		logger.debug("Message[From:{}] accpeted: {}", message.getFrom(),
				message.getUid());
		executorService.execute(new Runnable() {

			@Override
			public void run() {
				messageHandler.onMessage(message);
			}

		});
	}
}
