package com.babeeta.butterfly;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Map;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.babeeta.butterfly.MessageRouting.Message;
import com.google.protobuf.ByteString;

public class MessageSelectorTest {

	private MessageSelector messageSelector;
	private MessageHandler messageHandler;
	private IMocksControl mocksControl;

	@Before
	public void setUp() throws Exception {
		mocksControl = createControl();
		messageSelector = new MessageSelector();
		messageHandler = mocksControl.createMock(MessageHandler.class);
	}

	@Test
	public void testOnMessage() throws RecipientAlreadyRegisteredException {
		messageSelector.register("a", messageHandler);
		messageHandler.onMessage(isA(Message.class));
		expectLastCall().once();
		mocksControl.replay();
		Message message = Message.newBuilder()
				.setContent(ByteString.copyFrom(new byte[0]))
				.setDate(12345566L)
				.setFrom("FROM")
				.setTo("a@b.com")
				.setUid("UID")
				.build();
		messageSelector.onMessage(message);
	}

	@Test
	public void testOnMessage收件人不带AT符号时消息会被丢弃() {
		mocksControl.replay();
		Message message = Message.newBuilder()
				.setContent(ByteString.copyFrom(new byte[0]))
				.setDate(12345566L)
				.setFrom("FROM")
				.setTo("a@b.com")
				.setUid("UID")
				.build();
		messageSelector.onMessage(message);
	}

	@Test
	public void testOnMessage收件人未注册并且默认Handler也没注册时消息被丢弃() {
		Message message = Message.newBuilder()
				.setContent(ByteString.copyFrom(new byte[0]))
				.setDate(12345566L)
				.setFrom("FROM")
				.setTo("a@b.com")
				.setUid("UID")
				.build();
		messageSelector.onMessage(message);
	}

	@Test
	public void testOnMessage收件人未注册时会调用默认Handler处理消息() {
		messageHandler.onMessage(isA(Message.class));
		expectLastCall().once();
		mocksControl.replay();

		messageSelector.setDefaultMessageHandler(messageHandler);

		Message message = Message.newBuilder()
				.setContent(ByteString.copyFrom(new byte[0]))
				.setDate(12345566L)
				.setFrom("FROM")
				.setTo("a@b.com")
				.setUid("UID")
				.build();
		messageSelector.onMessage(message);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRegister() throws RecipientAlreadyRegisteredException {
		Map<String, MessageHandler> map = (Map<String, MessageHandler>) ReflectionTestUtils
				.getField(
						messageSelector, "recipientMap");

		assertTrue(map.isEmpty());

		mocksControl.replay();

		messageSelector.register("recipient", messageHandler);

		assertEquals(messageHandler, map.get("recipient"));
	}

	@SuppressWarnings("unchecked")
	@Test(expected = RecipientAlreadyRegisteredException.class)
	public void testRegister已经注册过时会抛异常()
			throws RecipientAlreadyRegisteredException {
		Map<String, MessageHandler> map = (Map<String, MessageHandler>) ReflectionTestUtils
				.getField(
						messageSelector, "recipientMap");
		map.put("recipient", messageHandler);

		mocksControl.replay();

		messageSelector.register("recipient", messageHandler);
	}
}