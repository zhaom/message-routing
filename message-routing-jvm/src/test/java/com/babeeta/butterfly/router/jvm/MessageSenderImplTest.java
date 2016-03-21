package com.babeeta.butterfly.router.jvm;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.net.UnknownServiceException;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import com.babeeta.butterfly.MessageFuture;
import com.babeeta.butterfly.MessageRouting.Message;
import com.google.protobuf.ByteString;

public class MessageSenderImplTest {

	static Message buildMessage(String to) {
		Message msg = Message.newBuilder()
								.setTo(to)
								.setUid("UID")
								.setContent(ByteString.copyFrom(new byte[0]))
								.setDate(System.currentTimeMillis())
								.setFrom("FROM")
								.build();
		return msg;
	}

	private MessageSenderImpl impl = null;
	private IMocksControl mocksControl = null;

	private DNS dns = null;

	@Before
	public void setUp() throws Exception {
		mocksControl = createControl();
		dns = mocksControl.createMock(DNS.class);

		impl = new MessageSenderImpl(dns);
	}

	@Test
	public void testSend当message对象的to属性不包含AT符号时future会使用IllegalArgumentException报告失败() {
		testSend使用錯誤的to屬性("abcdef");
	}

	@Test
	public void testSend当没有找到对应域名的Server实例时future会使用UnknownServiceException报告失败() {
		Message msg = buildMessage("aa@dummy");
		expect(dns.resolve("dummy")).andReturn(null).once();
		mocksControl.replay();

		MessageFuture future = impl.send(msg);
		assertNotNull(future);
		assertEquals(false, future.isSuccess());
		assertEquals(true, future.isDone());
		assertNotNull(future.getCause());
		assertTrue(future.getCause() instanceof UnknownServiceException);
	}

	@Test
	public void testSend成功() {
		final Message msg = buildMessage("recipient@dev");
		expect(dns.resolve("dev")).andReturn(new MessageService() {

			@Override
			public void commit(Message message) {
				assertSame(msg, message);
			}

		}).once();
		mocksControl.replay();

		MessageFuture future = impl.send(msg);
		assertNotNull(future);
		assertEquals(true, future.isSuccess());
		assertEquals(true, future.isDone());
		assertNull(future.getCause());
	}

	private void testSend使用錯誤的to屬性(String to) {
		Message msg = buildMessage(to);
		MessageFuture future = impl.send(msg);

		assertNotNull(future);
		assertEquals(false, future.isSuccess());
		assertEquals(true, future.isDone());
		assertNotNull(future.getCause());
		assertTrue(future.getCause() instanceof IllegalArgumentException);
	}
}
