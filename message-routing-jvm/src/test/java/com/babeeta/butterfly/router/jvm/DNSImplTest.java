package com.babeeta.butterfly.router.jvm;

import static org.junit.Assert.*;

import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class DNSImplTest {

	DNSImpl impl = null;
	Map<String, MessageService> routingTable = null;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		impl = DNSImpl.getDefaultInstance();
		routingTable = (Map<String, MessageService>) ReflectionTestUtils
				.getField(impl, "routingTable");
		routingTable.clear();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRegister参数domain为空时会抛出IllegalArgumentException()
			throws IllegalArgumentException,
			DomainAlreadyRegisteredException {
		MessageService s = EasyMock.createMock(MessageService.class);
		impl.register(null, s);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRegister参数domain为空白字符串时会抛出IllegalArgumentException()
			throws IllegalArgumentException,
			DomainAlreadyRegisteredException {
		MessageService s = EasyMock.createMock(MessageService.class);
		impl.register("  \t", s);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRegister参数service为空时会抛出IllegalArgumentException()
			throws IllegalArgumentException,
			DomainAlreadyRegisteredException {
		impl.register("domain", null);
	}

	@Test
	public void testRegister正常情况() throws IllegalArgumentException,
			DomainAlreadyRegisteredException {
		MessageService s = EasyMock.createMock(MessageService.class);
		impl.register("domain", s);
		assertEquals(1, routingTable.size());
		assertSame(s, routingTable.get("domain"));
	}

	@Test(expected = DomainAlreadyRegisteredException.class)
	public void testRegister重复注册时会抛出DomainAlreadyRegisteredException()
			throws IllegalArgumentException,
			DomainAlreadyRegisteredException {
		MessageService s = EasyMock.createMock(MessageService.class);
		impl.register("domain", s);
		MessageService s2 = EasyMock.createMock(MessageService.class);
		try {
			impl.register("domain", s2);
		} catch (DomainAlreadyRegisteredException e) {
			assertEquals("domain", e.getDomain());
			throw e;
		}
	}

	@Test
	public void testResolve() {
		MessageService s = EasyMock.createMock(MessageService.class);
		assertNull(impl.resolve("domain"));
		routingTable.put("domain", s);
		assertSame(s, impl.resolve("domain"));
	}

	@Test
	public void testUnregister() {
		MessageService s = EasyMock.createMock(MessageService.class);
		assertEquals(false, impl.unregister("domain", s));
		routingTable.put("domain", s);
		assertEquals(true, impl.unregister("domain", s));
	}

}
