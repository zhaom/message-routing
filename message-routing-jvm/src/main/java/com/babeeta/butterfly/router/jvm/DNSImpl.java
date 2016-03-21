package com.babeeta.butterfly.router.jvm;

import java.util.concurrent.ConcurrentHashMap;

/**
 * JVM 内部的DNS实现
 * 
 * @author leon
 * 
 */
public class DNSImpl implements DNS {
	private final ConcurrentHashMap<String, MessageService> routingTable = new ConcurrentHashMap<String, MessageService>();

	private static final DNSImpl defaultInstance = new DNSImpl();

	public static DNSImpl getDefaultInstance() {
		return defaultInstance;
	}

	private DNSImpl() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.babeeta.butterfly.router.jvm.DNS#register(java.lang.String,
	 * com.babeeta.butterfly.router.jvm.Service)
	 */
	@Override
	public void register(String domain, MessageService service)
			throws IllegalArgumentException, DomainAlreadyRegisteredException {
		checkDomainAndServer(domain, service);

		if (routingTable.putIfAbsent(domain, service) != null) {
			throw new DomainAlreadyRegisteredException(domain);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.babeeta.butterfly.router.jvm.DNS#resolve(java.lang.String)
	 */
	@Override
	public MessageService resolve(String domain) {
		return routingTable.get(domain);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.babeeta.butterfly.router.jvm.DNS#unregister(java.lang.String,
	 * com.babeeta.butterfly.router.jvm.Service)
	 */
	@Override
	public boolean unregister(String domain, MessageService service)
			throws IllegalArgumentException {
		checkDomainAndServer(domain, service);

		return routingTable.remove(domain, service);
	}

	private void checkDomainAndServer(String domain, MessageService service) {
		if (domain == null || domain.trim().equals("") || service == null) {
			throw new IllegalArgumentException(
					"Domain and Server cannot be null");
		}
	}
}
