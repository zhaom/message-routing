package com.babeeta.butterfly.router.jvm;


public class Server {
	private final MessageService service;
	private final String domain;
	private final DNS dns;

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
	 *             域null名为空时会抛出
	 * @throws DomainAlreadyRegisteredException
	 *             域名已经被注册了
	 */
	public Server(DNS dns, String domain, MessageService service)
			throws IllegalArgumentException,
			DomainAlreadyRegisteredException {
		super();
		this.dns = dns;
		this.domain = domain;
		this.service = service;
		dns.register(domain, service);
	}

	/**
	 * 停止服务
	 */
	public void shutdown() {
		this.dns.unregister(domain, service);
	}
}