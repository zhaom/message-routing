package com.babeeta.butterfly.router.network;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.easymock.Capture;
import org.easymock.IMocksControl;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.babeeta.butterfly.MessageFutureListener;

public class MessageFutureImplTest {

	private static final long _1000L = 1000L;
	private MessageFutureImpl impl = null;
	private IMocksControl mocksControl = null;
	private ChannelFuture channelFuture = null;
	private Capture<ChannelFutureListener> channelFutureListenerCapture = null;

	@Before
	public void setUp() throws Exception {
		Logger logger = LogManager.getLogManager().getLogger(Logger.GLOBAL_LOGGER_NAME);
		logger.setLevel(Level.FINEST);
		
		mocksControl = createControl();
		channelFutureListenerCapture = new Capture<ChannelFutureListener>();
		impl = new MessageFutureImpl();
		channelFuture = mocksControl.createMock(ChannelFuture.class);
		channelFuture.addListener(capture(channelFutureListenerCapture));
		expectLastCall().atLeastOnce();
	}
	
	@Test
	public void testAddListener操作正在进行中()
			throws IllegalArgumentException,
				SecurityException, IllegalAccessException, NoSuchFieldException {
		expect(channelFuture.isDone()).andReturn(false).times(2);
		MessageFutureListener messageFutureListener = mocksControl
				.createMock(MessageFutureListener.class);
		mocksControl.replay();
		impl.setChannelFuture(channelFuture);
		
		impl.addListener(messageFutureListener);

		@SuppressWarnings("unchecked")
		Set<MessageFutureListener> listenerSet =
			(Set<MessageFutureListener>) ReflectionTestUtils.getField(impl, "listenerSet");

		assertNotNull(listenerSet);
		assertEquals(1, listenerSet.size());
		assertSame(messageFutureListener, listenerSet.iterator().next());
	}
	@Test
	public void testAddListener根据channelFuture判断操作已经完成()
			throws IllegalArgumentException,
				SecurityException, IllegalAccessException, NoSuchFieldException {
		expect(channelFuture.isDone()).andReturn(false).times(2);
		MessageFutureListener messageFutureListener = mocksControl
				.createMock(MessageFutureListener.class);
		messageFutureListener.operationComplete(impl);
		expectLastCall().once();
		mocksControl.replay();
		impl.setChannelFuture(channelFuture);
		
		impl.addListener(messageFutureListener);
	}
	@Test
	public void testAddListener根据getCause判断操作已经完成(){
		mocksControl.reset();
		MessageFutureListener messageFutureListener = mocksControl
				.createMock(MessageFutureListener.class);
		messageFutureListener.operationComplete(impl);
		expectLastCall().once();
		mocksControl.replay();
		impl.setCause(new Exception());
		
		impl.addListener(messageFutureListener);
		
	}
	
	@Test
	public void testAddListener添加后操作完成所有Listener的operationComplete应该被调用()
		throws Exception{
		expect(channelFuture.isDone()).andReturn(false).times(3);
		expect(channelFuture.isDone()).andReturn(true).times(2);
		MessageFutureListener messageFutureListener1 = mocksControl
				.createMock(MessageFutureListener.class);
		MessageFutureListener messageFutureListener2 = mocksControl
				.createMock(MessageFutureListener.class);
		messageFutureListener1.operationComplete(impl);
		expectLastCall().once();
		messageFutureListener2.operationComplete(impl);
		expectLastCall().once();
		
		mocksControl.replay();
		impl.setChannelFuture(channelFuture);
		
		impl.addListener(messageFutureListener1);
		impl.addListener(messageFutureListener2);

		assertTrue(channelFutureListenerCapture.hasCaptured());
		channelFutureListenerCapture.getValue().operationComplete(channelFuture);
	}

	@Test
	public void testAwaitLong內部channelFuture正常工作的情況() throws InterruptedException {
		expect(channelFuture.await(_1000L)).andReturn(true).once();
		expect(channelFuture.isDone()).andReturn(false).times(2);
		mocksControl.replay();
		impl.setChannelFuture(channelFuture);
		
		assertEquals(true, impl.await(_1000L));
	}
	
	@Test(expected=InterruptedException.class)
	public void testAwaitLong內部channelFuture的await方法拋出了異常() throws InterruptedException{
		expect(channelFuture.await(_1000L)).andThrow(new InterruptedException()).once();
		expect(channelFuture.isDone()).andReturn(false).once();
		mocksControl.replay();
		impl.setChannelFuture(channelFuture);
		
		impl.await(1000L);
	}
	
	@Test
	public void testAwaitLong內部沒有ChannelFuture的情況() throws InterruptedException{
		mocksControl.reset();
		mocksControl.replay();
		assertEquals(false, impl.await(_1000L));
	}
	
	@Test
	public void testAwait內部channelFuture正常工作的情況() throws InterruptedException {
		expect(channelFuture.await()).andReturn(channelFuture).once();
		expect(channelFuture.isDone()).andReturn(false).once();
		mocksControl.replay();
		impl.setChannelFuture(channelFuture);
		
		impl.await();
	}

	@Test(expected=InterruptedException.class)
	public void testAwait內部channelFuture的await方法拋出了異常() throws InterruptedException{
		expect(channelFuture.await()).andThrow(new InterruptedException());
		expect(channelFuture.isDone()).andReturn(false).once();
		mocksControl.replay();
		impl.setChannelFuture(channelFuture);
		
		impl.await();
	}

	
	@Test
	public void testAwait內部沒有ChannelFuture的情況() throws InterruptedException{
		mocksControl.reset();
		mocksControl.replay();
		impl.await();
	}

	@Test
	public void testGetCause带有内部channelFuture() {
		expect(channelFuture.getCause()).andReturn(new Exception("ABC")).once();
		expect(channelFuture.isDone()).andReturn(false).once();
		mocksControl.replay();
		impl.setChannelFuture(channelFuture);
		
		assertEquals("ABC", impl.getCause().getMessage());
	}
	
	@Test
	public void testGetCause没有内部的channelFuture() {
		assertNull(impl.getCause());
		impl.setCause(new Exception("XYZ"));
		assertEquals("XYZ", impl.getCause().getMessage());
	}

	@Test
	public void testIsDone有ChannelFuture的时候() {
		expect(channelFuture.isDone()).andReturn(false).once();
		expect(channelFuture.isDone()).andReturn(true).once();
		mocksControl.replay();
		impl.setChannelFuture(channelFuture);
		
		assertEquals(true, impl.isDone());
	}

	@Test
	public void testIsDone没有ChannelFuture的时候() {
		assertEquals(false, impl.isDone());
		impl.setCause(new Exception());
		assertEquals(true, impl.isDone());
	}
	

	@Test
	public void testIsSuccess有channelFuture的时候() {
		expect(channelFuture.isSuccess()).andReturn(true).once();
		expect(channelFuture.isDone()).andReturn(false).once();
		mocksControl.replay();
		impl.setChannelFuture(channelFuture);
		
		assertEquals(true, impl.isSuccess());
	}
	
	@Test
	public void testIsSuccess没有channelFuture的时候() {
		assertEquals(false, impl.isSuccess());
	}

	@Test
	public void testRemoveListener() {
		expect(channelFuture.isDone()).andReturn(false).times(2);
		MessageFutureListener messageFutureListener1 = mocksControl
				.createMock(MessageFutureListener.class);
		mocksControl.replay();
		impl.setChannelFuture(channelFuture);
		
		@SuppressWarnings("unchecked")
		Set<MessageFutureListener> listenerSet =
			(Set<MessageFutureListener>) ReflectionTestUtils.getField(impl, "listenerSet");
		assertTrue(listenerSet.isEmpty());
		
		impl.addListener(messageFutureListener1);
		assertEquals(1, listenerSet.size());
		assertEquals(true, listenerSet.contains(messageFutureListener1));
		assertEquals(true, impl.removeListener(messageFutureListener1));
		assertTrue(listenerSet.isEmpty());
		assertEquals(false, impl.removeListener(messageFutureListener1));
	}

}
