package com.babeeta.butterfly.router.jvm;

public interface DNS {

	/**
	 * 域名注册
	 * 
	 * @param domain
	 *            要注册域名
	 * @param server
	 *            在此域名上提供的服务
	 * @throws IllegalArgumentException
	 *             如果域名或服务为null
	 * @throws DomainAlreadyRegisteredException
	 *             如果域名已经被注册
	 */
	void register(String domain, MessageService service)
			throws IllegalArgumentException, DomainAlreadyRegisteredException;

	/**
	 * service = mocksControl.createMock(Service.class);
	 * 
	 * 通过域名获取服务
	 * 
	 * @param domain
	 *            域名
	 * @return 绑定在此域名上的服务。或者null， 如果没有绑定
	 */
	MessageService resolve(String domain);

	/**
	 * 取消域名注册
	 * 
	 * @param domain
	 *            域名
	 * @param server
	 *            绑定在此域名上的服务
	 * @return 是否取消成功。例如当服务没有注册在此域名，或域名不存再时都会返回false。
	 * @throws IllegalArgumentException
	 *             如果域名或服务为null
	 */
	boolean unregister(String domain, MessageService service)
			throws IllegalArgumentException;

}