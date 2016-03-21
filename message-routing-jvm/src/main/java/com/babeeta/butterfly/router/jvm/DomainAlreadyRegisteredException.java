package com.babeeta.butterfly.router.jvm;

/**
 * 域名已经再JVM范围内被注册
 * 
 * @author leon
 * 
 */
public class DomainAlreadyRegisteredException extends Exception {
	private static final long serialVersionUID = -2931280816848294178L;

	private final String domain;

	public DomainAlreadyRegisteredException(String domain) {
		super(domain + " already exists.");
		this.domain = domain;
	}

	/**
	 * 发生冲突的域名
	 * 
	 * @return
	 */
	public String getDomain() {
		return this.domain;
	}

}
