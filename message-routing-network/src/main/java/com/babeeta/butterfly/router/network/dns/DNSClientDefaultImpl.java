package com.babeeta.butterfly.router.network.dns;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

public class DNSClientDefaultImpl implements DNSClient {

	private final InetAddress dnsService;
	private static final String DEFAULT_SERVICE_PREFIX = "_mr._tcp.";

	public DNSClientDefaultImpl() {
		this(null);
	}

	DNSClientDefaultImpl(InetAddress dnsService) {
		super();
		this.dnsService = dnsService;
	}

	@Override
	public InetSocketAddress resove(String serviceDomain)
			throws UnknownHostException {
		String serviceName = DEFAULT_SERVICE_PREFIX + serviceDomain;
		Lookup lookup;
		try {
			lookup = new Lookup(serviceName,
					Type.SRV);
		} catch (TextParseException e) {
			throw new UnknownHostException(serviceName);
		}
		if (dnsService == null) {
			lookup.setResolver(new SimpleResolver());
		} else {
			// 用于测试
			lookup.setResolver(new SimpleResolver(dnsService.getHostName()));
		}

		Record[] response = lookup.run();

		if (response == null || response.length == 0) {
			throw new UnknownHostException(serviceName);
		} else {
			return new InetSocketAddress(response[0].getAdditionalName()
					.toString(), ((SRVRecord) response[0]).getPort());
		}
	}
}
