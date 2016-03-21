package com.babeeta.butterfly;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.easymock.Capture;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import com.babeeta.butterfly.MessageRouting.Message;
import com.google.protobuf.ByteString;

public class AbstractMessageRouterTest {

	private AbstractMessageRouter router = null;
	private MessageSender messageSender = null;
	private IMocksControl mocksControl = null;

	@Before
	public void setup() {
		LogManager.getLogManager().getLogger(Logger.GLOBAL_LOGGER_NAME)
				.setLevel(Level.ALL);

		mocksControl = createControl();
		messageSender = mocksControl.createMock(MessageSender.class);
	}

	@Test
	public void testOnMessage() {
		router = new AbstractMessageRouter(messageSender) {

			@Override
			protected Message transform(Message message) {
				assertEquals("TO", message.getTo());
				return message.toBuilder()
						.setTo(message.getTo() + "$transformed").build();
			}
		};
		Capture<Message> messageCapture = new Capture<MessageRouting.Message>();
		Capture<MessageFutureListener> messageFutureListenerCapture = new Capture<MessageFutureListener>();
		MessageFuture messageFuture = mocksControl
				.createMock(MessageFuture.class);
		expect(messageSender.send(capture(messageCapture))).andReturn(
				messageFuture).once();
		expect(messageFuture.addListener(capture(messageFutureListenerCapture)))
				.andReturn(messageFuture)
				.once();
		expect(messageFuture.isSuccess()).andReturn(true).once();
		expect(messageFuture.isSuccess()).andReturn(false).once();
		expect(messageFuture.getCause()).andReturn(new Exception("H")).once();
		mocksControl.replay();

		router.onMessage(Message.newBuilder()
				.setTo("TO")
				.setUid("UID")
				.setDate(System.currentTimeMillis())
				.setFrom("FROM")
				.setContent(ByteString.copyFrom(new byte[0]))
				.build());

		assertTrue(messageCapture.hasCaptured());
		assertEquals("TO$transformed", messageCapture.getValue().getTo());
		assertTrue(messageFutureListenerCapture.hasCaptured());
		MessageFutureListener listener = messageFutureListenerCapture
				.getValue();
		assertNotNull(listener);
		listener.operationComplete(messageFuture);
		listener.operationComplete(messageFuture);
	}

	@Test
	public void testOnMessage如果转换有异常发生则会丢弃消息() {
		router = new AbstractMessageRouter(messageSender) {

			@Override
			protected Message transform(Message message) {
				throw new IllegalArgumentException();
			}
		};

		router.onMessage(Message.newBuilder()
				.setTo("TO")
				.setUid("UID")
				.setDate(System.currentTimeMillis())
				.setFrom("FROM")
				.setContent(ByteString.copyFrom(new byte[0]))
				.build());
	}

	@Test
	public void 当transform返回Null时需要对消息做丢弃处理() {
		router = new AbstractMessageRouter(messageSender) {

			@Override
			protected Message transform(Message message) {
				assertEquals("TO", message.getTo());
				return null;
			}
		};
		mocksControl.replay();
		router.onMessage(Message.newBuilder()
				.setTo("TO")
				.setUid("UID")
				.setDate(System.currentTimeMillis())
				.setFrom("FROM")
				.setContent(ByteString.copyFrom(new byte[0]))
				.build());
	}
}
