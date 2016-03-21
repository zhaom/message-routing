package com.babeeta.butterfly.router.jvm;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.babeeta.butterfly.AbstractMessageRouter;
import com.babeeta.butterfly.MessageHandler;
import com.babeeta.butterfly.MessageRouting.Message;
import com.babeeta.butterfly.MessageSender;
import com.google.protobuf.ByteString;

public class IntegrationTest {
	private ExecutorService executorService = null;
	private MessageSenderImpl messageSender = null;
	private List<Server> serverList = new LinkedList<Server>();
	private DNS dns = null;

	@Before
	public void setup() throws Exception {
		dns = DNSImpl.getDefaultInstance();
		executorService = Executors.newCachedThreadPool();
		messageSender = new MessageSenderImpl(dns);

		setupService(new DevRouter(messageSender), "dev");
		setupService(new GatewayRouter(messageSender), "gateway.dev");
		setupService(new SecondGatewayRouter(messageSender),
				"router-0.gateway.dev");
		setupService(new SecondGatewayRouter(messageSender),
				"router-1.gateway.dev");
		setupService(new DeviceGatewayRouter(), "0.gateway.dev");
		setupService(new DeviceGatewayRouter(), "1.gateway.dev");
	}

	@After
	public void teardown() throws Exception {
		for (Server server : serverList) {
			server.shutdown();
		}
		executorService.shutdown();
	}

	@Test
	public void test() throws Exception {
		long begin = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			Message msg = buildMessage(i);
			assertEquals(true, messageSender.send(msg).isSuccess());
		}
		long sendingTime = System.currentTimeMillis() - begin;

		System.out.printf("[Speed]Sender:%2f/s\n",
				((double) 1) / sendingTime);

		Long endTime = DeviceGatewayRouter.signalQueue.poll(10L,
				TimeUnit.SECONDS);
		assertNotNull("Our system is slow.", endTime);

		System.out.printf("[Speed]Sender:%2f/s\n",
				1000 / ((double) sendingTime / 1000));
		System.out.printf("[Speed]Total:%2f/s\n", 1000
				/ (((double) endTime - begin) / 1000));

	}

	private Message buildMessage(int i) {
		return Message.newBuilder()
						.setUid(String.valueOf(i))
						.setContent(ByteString.copyFrom(new byte[0]))
						.setDate(System.currentTimeMillis())
						.setFrom("FROM")
						.setTo("recipient@dev")
						.build();
	}

	private void setupService(MessageHandler messageHandler, String domain)
			throws IllegalArgumentException, DomainAlreadyRegisteredException {
		MessageService messageService = new MessageServiceImpl(messageHandler,
				executorService);
		serverList.add(new Server(dns, domain, messageService));
	}
}

class Address {
	String name;
	String domain;

	Address(String address) {
		int atIndex = address.indexOf("@");
		name = address.substring(0, atIndex);
		domain = address.substring(atIndex + 1, address.length());
	}
}

class DeviceGatewayRouter implements MessageHandler {
	private static final Logger logger = LoggerFactory
			.getLogger(DeviceGatewayRouter.class);
	static final LinkedList<Message> result = new LinkedList<Message>();
	static final BlockingQueue<Long> signalQueue = new LinkedBlockingQueue<Long>();

	@Override
	public void onMessage(Message message) {
		logger.debug("On message:{}", message.getUid());
		result.push(message);
		if (result.size() == 1000) {
			try {
				signalQueue.put(System.currentTimeMillis());
			} catch (InterruptedException e) {
				fail(e.getMessage());
			}
		}
	}

}

class DevRouter extends AbstractMessageRouter {

	public DevRouter(MessageSender messageSender) {
		super(messageSender);
	}

	@Override
	protected Message transform(Message message) {
		Address add = new Address(message.getTo());
		return message.toBuilder()
				.setTo(add.name.hashCode() + "@gateway.dev")
				.addVia(getClass().getName()).build();
	}

}

class GatewayRouter extends AbstractMessageRouter {

	public GatewayRouter(MessageSender messageSender) {
		super(messageSender);
	}

	@Override
	protected Message transform(Message message) {
		Address addr = new Address(message.getTo());
		return message
				.toBuilder()
				.setTo(addr.name + "@router-"
						+ (Math.abs(Integer.valueOf(addr.name.hashCode())) % 2)
						+ ".gateway.dev")
				.addVia(this.getClass().getName()).build();
	}

}

class SecondGatewayRouter extends AbstractMessageRouter {

	public SecondGatewayRouter(MessageSender messageSender) {
		super(messageSender);
	}

	@Override
	protected Message transform(Message message) {
		Address addr = new Address(message.getTo());
		return message
				.toBuilder()
				.setTo(addr.name + "@"
						+ Math.abs((Integer.valueOf(addr.name.hashCode()) % 2))
						+ ".gateway.dev")
				.addVia(this.getClass().getName()).build();
	}

}