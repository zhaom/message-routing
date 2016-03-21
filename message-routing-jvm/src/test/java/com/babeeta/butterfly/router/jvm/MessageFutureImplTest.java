package com.babeeta.butterfly.router.jvm;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.babeeta.butterfly.MessageFuture;
import com.babeeta.butterfly.MessageFutureListener;

public class MessageFutureImplTest {

	MessageFutureImpl impl = null;

	@Before
	public void setUp() throws Exception {
		impl = new MessageFutureImpl();
	}

	@Test
	public void testAddListener操作完成时Listener会被即刻调用() {
		ReflectionTestUtils.setField(impl, "done", true);
		final Thread t = Thread.currentThread();
		impl.addListener(new MessageFutureListener() {

			@Override
			public void operationComplete(MessageFuture future) {
				assertSame(Thread.currentThread(), t);
			}
		});
	}

	@Test
	public void testAwaitLong立即返回没动作() throws InterruptedException {
		impl.await();
	}

	@Test
	public void testAwait立即返回没动作() throws InterruptedException {
		assertEquals(true, impl.await(1000L));
	}

	@Test
	public void testGetCause() {
		Exception e = new Exception();
		ReflectionTestUtils.setField(impl, "cause", e);
		assertSame(e, impl.getCause());
	}

	@Test
	public void testIsDone任务提交但失败后返回true() {
		impl.failed(new Exception());
		assertEquals(true, impl.isDone());
	}

	@Test
	public void testIsDone任务提交成功后后返回true() {
		impl.success();
		assertEquals(true, impl.isDone());
	}

	@Test
	public void testIsSuccess有cause时返回false() {
		impl.failed(new Exception());
		assertEquals(false, impl.isSuccess());
	}

	@Test
	public void testIsSuccess没有cause是返回true() {
		impl.success();
		assertEquals(true, impl.isSuccess());
	}

	@Test
	public void testRemoveListener什么都不会发生() {
		impl.removeListener(null);
	}

}