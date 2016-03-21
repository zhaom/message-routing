package com.babeeta.butterfly.misc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用于解析cid.aid@domain或did.cid.aid@domain类型的地址。
 * 
 * @author leon
 * 
 */
public class Address {
	public final String deviceId;
	public final String clientId;
	public final String applicationId;
	public final String domain;
	private static final Pattern REGX_ADDR = Pattern
			.compile("(([0-9a-z]+)\\.)?([0-9a-z]+)\\.([0-9a-z]+)@(.+)");

	public Address(String addr) {
		Matcher matcher = null;
		if (addr == null || addr.trim().length() == 0
				|| !(matcher = REGX_ADDR.matcher(addr)).matches()) {
			throw new IllegalArgumentException();
		} else {
			deviceId = matcher.group(2);
			clientId = matcher.group(3);
			applicationId = matcher.group(4);
			domain = matcher.group(5);
		}
	}

	/**
	 * 使用新的域名构建地址
	 * 
	 * @param domain
	 * @return
	 */
	public String buildAddress(String domain) {
		return buildAddress(deviceId, domain);
	}

	/**
	 * 为地址追加DeviceID并替换域名
	 * 
	 * @param deviceId
	 * @param domain
	 * @return
	 */
	public String buildAddress(String deviceId, String domain) {
		StringBuilder buf = new StringBuilder();
		if (deviceId != null && deviceId.trim().length() != 0) {
			buf.append(deviceId)
					.append(".");
		}
		return buf.append(clientId)
				.append(".")
				.append(applicationId)
				.append("@")
				.append(domain).toString();
	}

}