package com.babeeta.butterfly.router.network.dns;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public interface DNSClient {
	InetSocketAddress resove(String serviceDomain) throws UnknownHostException;
}
