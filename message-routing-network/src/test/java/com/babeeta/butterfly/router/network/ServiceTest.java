package com.babeeta.butterfly.router.network;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.babeeta.butterfly.MessageHandler;
import com.babeeta.butterfly.MessageRouting.Message;
import com.babeeta.butterfly.router.network.dns.DNSClient;
import com.google.protobuf.ByteString;

public class ServiceTest {

	private Service server = null;
	private IMocksControl mocksControl = null;
	private MessageHandler messageHandler = null;
	private ExecutorService executorService = null;
	private Socket clientSocket;
	private BlockingQueue<Message> received;

	@Before
	public void setup() throws UnknownHostException {
		executorService = Executors.newCachedThreadPool();
		mocksControl = createControl();
		messageHandler = mocksControl.createMock(MessageHandler.class);
		DNSClient dnsClient = mocksControl.createMock(DNSClient.class);

		expect(dnsClient.resove(isA(String.class))).andReturn(
				new InetSocketAddress(5757)).once();

		received = new LinkedBlockingQueue<Message>();
		messageHandler = new MessageHandler() {

			@Override
			public void onMessage(Message message) {
				received.offer(message);
			}
		};

		mocksControl.replay();
		server = new Service(dnsClient, messageHandler, executorService,
				executorService);
	}

	@Test(expected = IOException.class)
	public void shutdown() throws Exception {
		server.start("aa");
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress("localhost", 5757), 1000);
		socket.close();
		server.shutdownGraceFully();
		socket.connect(new InetSocketAddress("localhost", 5757), 1000);
		assertTrue(executorService.awaitTermination(10L, TimeUnit.SECONDS));
	}

	@After
	public void teardown() throws IOException {
		if (executorService != null && !executorService.isShutdown()) {
			executorService.shutdown();
		}
		if (clientSocket != null && clientSocket.isConnected()) {
			clientSocket.close();
		}
	}

	@Test
	public void test() throws Exception {
		server.start("bb");

		clientSocket = new Socket();
		clientSocket.connect(new InetSocketAddress("localhost", 5757));

		assertTrue(clientSocket.isConnected());

		Message msg = Message.newBuilder()
								.setUid("UID")
								.setTo("TO")
								.setFrom("FROM")
								.setDate(System.currentTimeMillis())
								.setContent(ByteString.copyFrom(new byte[0]))
								.build();

		DataOutputStream out = new DataOutputStream(
				clientSocket.getOutputStream());
		msg.writeDelimitedTo(out);

		msg = msg.toBuilder().setUid("UID-X").build();
		msg.writeDelimitedTo(out);

		Message receivedMsg = received.poll(1, TimeUnit.SECONDS);
		assertNotNull(receivedMsg);
		assertEquals("UID", receivedMsg.getUid());

		receivedMsg = received.poll(1, TimeUnit.SECONDS);
		assertNotNull(receivedMsg);
		assertEquals("UID-X", receivedMsg.getUid());

		long lastUID = -1;
		for (int i = 0; i < 1000; i++) {
			lastUID = System.nanoTime() + i;
			msg = msg.toBuilder().setUid(String.valueOf(lastUID)).build();
			msg.writeDelimitedTo(out);
			out.flush();
		}

		for (int i = 0; i < 999; i++) {
			assertNotNull(received.poll(1, TimeUnit.SECONDS));
		}

		assertEquals(String.valueOf(lastUID), received
				.poll(1, TimeUnit.SECONDS).getUid());

		msg = msg.toBuilder().setUid("Co").build();
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		msg.writeDelimitedTo(bout);
		bout.close();
		byte[] data = bout.toByteArray();
		out.write(data, 0, 1);
		out.flush();
		out.write(data, 1, 1);
		out.flush();

		out.write(data, 2, data.length - 2);
		out.flush();
		assertEquals("Co", received
				.poll(1, TimeUnit.SECONDS).getUid());

		Message.newBuilder()
				.setUid("heartbeat")
				.setDate(-1)
				.setFrom("")
				.setTo("")
				.setContent(ByteString.EMPTY)
				.build().writeDelimitedTo(out);
		out.flush();

		assertEquals(0, received.size());

		clientSocket.close();
	}
}