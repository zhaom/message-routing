package com.babeeta.butterfly.rpc;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;

import org.easymock.Capture;
import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.babeeta.butterfly.MessageFuture;
import com.babeeta.butterfly.MessageFutureListener;
import com.babeeta.butterfly.MessageRouting.Message;
import com.babeeta.butterfly.MessageSender;
import com.google.protobuf.ByteString;

public class MessageBasedRPCServiceTest {

	private MessageBasedRPCService service;
	private MessageSender messageSender;
	private MessageFuture messageFuture;
	private IMocksControl mocksControl;
	private ScheduledFuture<?> scheduledFuture;

	@Before
	public void setUp() throws Exception {
		mocksControl = createControl();
		messageFuture = mocksControl.createMock(MessageFuture.class);
		messageSender = mocksControl.createMock(MessageSender.class);
		service = new MessageBasedRPCService(messageSender);
		scheduledFuture = mocksControl.createMock(ScheduledFuture.class);
	}

	@Test
	public void testInvoke() {
		Message call = Message.newBuilder()
				.setUid(String.valueOf(System.currentTimeMillis()))
				.setDate(System.currentTimeMillis())
				.setContent(ByteString.copyFrom(new byte[0]))
				.setFrom("From")
				.setTo("TO")
				.build();

		Capture<MessageFutureListener> capture = new Capture<MessageFutureListener>();
		RPCHandler handler = mocksControl.createMock(RPCHandler.class);

		expect(messageSender.send(call))
				.andReturn(messageFuture)
				.once();
		expect(messageFuture.addListener(capture(capture)))
				.andReturn(messageFuture)
				.once();
		expect(messageFuture.isSuccess())
				.andReturn(true).once();
		expect(messageFuture.isSuccess())
				.andReturn(false).atLeastOnce();
		final Exception e = new Exception();
		expect(messageFuture.getCause())
				.andReturn(e)
				.atLeastOnce();

		handler.exceptionCaught(same(call), same(e));
		expectLastCall().once();

		handler.exceptionCaught(call, e);
		expectLastCall().andThrow(new RuntimeException("Test."))
				.once();

		mocksControl.replay();
		service.invoke(call, handler);

		@SuppressWarnings("unchecked")
		Map<String, RPCInvoke> internalMap = (Map<String, RPCInvoke>) ReflectionTestUtils
				.getField(
						service, "rpcRegistry");

		assertTrue(capture.hasCaptured());
		assertTrue(internalMap.containsKey(call.getUid()));

		capture.getValue().operationComplete(messageFuture);
		assertTrue(internalMap.containsKey(call.getUid()));
		assertSame(internalMap.get(call.getUid()).handler, handler);

		internalMap.clear();
		capture.getValue().operationComplete(messageFuture);
		assertFalse(internalMap.containsKey(call.getUid()));

		capture.getValue().operationComplete(messageFuture);
		assertFalse(internalMap.containsKey(call.getUid()));
	}

	@Test
	public void testInvoke超时后会被清除掉() throws InterruptedException {
		Message call = Message.newBuilder()
				.setUid(String.valueOf(System.currentTimeMillis()))
				.setDate(System.currentTimeMillis())
				.setContent(ByteString.copyFrom(new byte[0]))
				.setFrom("From")
				.setTo("TO")
				.build();

		final Capture<MessageFutureListener> capture = new Capture<MessageFutureListener>();
		RPCHandler handler = mocksControl.createMock(RPCHandler.class);

		expect(messageSender.send(call))
				.andReturn(messageFuture)
				.once();
		expect(messageFuture.addListener(capture(capture)))
				.andAnswer(new IAnswer<MessageFuture>() {

					@Override
					public MessageFuture answer() throws Throwable {
						capture.getValue().operationComplete(messageFuture);
						return messageFuture;
					}
				}).once();
		expect(messageFuture.isSuccess())
				.andReturn(true).once();

		Capture<Throwable> throwableCapture = new Capture<Throwable>();
		handler.exceptionCaught(same(call), capture(throwableCapture));
		expectLastCall().once();

		mocksControl.replay();

		service.invokeTimeout = 1;

		service.invoke(call, handler);
		Thread.sleep(1020L);

		assertTrue(throwableCapture.hasCaptured());
		assertTrue(throwableCapture.getValue() instanceof TimeoutException);
	}

	@Test
	public void testOnMessage() {
		Message call = Message.newBuilder()
				.setUid(String.valueOf(System.currentTimeMillis()))
				.setDate(System.currentTimeMillis())
				.setContent(ByteString.copyFrom(new byte[0]))
				.setFrom("From")
				.setTo("TO")
				.build();
		Message response = Message.newBuilder()
				.setUid(String.valueOf(System.currentTimeMillis()))
				.setDate(System.currentTimeMillis())
				.setContent(ByteString.copyFrom(new byte[0]))
				.setFrom("From")
				.setTo("TO")
				.setReplyFor(call.getUid())
				.build();

		@SuppressWarnings("unchecked")
		Map<String, RPCInvoke> internalMap = (Map<String, RPCInvoke>) ReflectionTestUtils
				.getField(
						service, "rpcRegistry");

		RPCHandler handler = mocksControl.createMock(RPCHandler.class);
		internalMap.put(call.getUid(), new RPCInvoke(handler, scheduledFuture));

		expect(scheduledFuture.cancel(false))
				.andReturn(true)
				.once();

		handler.onMessage(same(response));
		expectLastCall().once();

		mocksControl.replay();
		service.onMessage(response);

		assertFalse(internalMap.containsKey(call.getUid()));
	}

	@Test
	public void testOnMessage如果Handler的onMessage方法抛出异常那么handler的exceptionCause将会被调用() {
		Message call = Message.newBuilder()
					.setUid(String.valueOf(System.currentTimeMillis()))
					.setDate(System.currentTimeMillis())
					.setContent(ByteString.copyFrom(new byte[0]))
					.setFrom("From")
					.setTo("TO")
					.build();
		Message response = Message.newBuilder()
					.setUid(String.valueOf(System.currentTimeMillis()))
					.setDate(System.currentTimeMillis())
					.setContent(ByteString.copyFrom(new byte[0]))
					.setFrom("From")
					.setTo("TO")
					.setReplyFor(call.getUid())
					.build();

		@SuppressWarnings("unchecked")
		Map<String, RPCInvoke> internalMap = (Map<String, RPCInvoke>) ReflectionTestUtils
				.getField(
						service, "rpcRegistry");

		RPCHandler handler = mocksControl.createMock(RPCHandler.class);
		internalMap.put(call.getUid(), new RPCInvoke(handler, scheduledFuture));

		handler.onMessage(same(response));
		RuntimeException e = new RuntimeException("Test For onMessage");
		expectLastCall().andThrow(e)
				.once();

		expect(scheduledFuture.cancel(false))
				.andReturn(false)
				.once();

		handler.exceptionCaught(call, e);
		expectLastCall().once();

		mocksControl.replay();
		service.onMessage(response);

		assertFalse(internalMap.containsKey(call.getUid()));

	}

	@Test
	public void testOnMessage没有对应的调用会导致消息被丢弃() {
		Message response = Message.newBuilder()
				.setUid(String.valueOf(System.currentTimeMillis()))
				.setDate(System.currentTimeMillis())
				.setContent(ByteString.copyFrom(new byte[0]))
				.setFrom("From")
				.setTo("TO")
				.setReplyFor("xxxxxxx")
				.build();

		mocksControl.replay();
		service.onMessage(response);
	}

}
