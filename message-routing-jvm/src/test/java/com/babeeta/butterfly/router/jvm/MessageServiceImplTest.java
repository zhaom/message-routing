package com.babeeta.butterfly.router.jvm;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutorService;

import org.easymock.Capture;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import com.babeeta.butterfly.MessageHandler;
import com.babeeta.butterfly.MessageRouting.Message;

public class MessageServiceImplTest {

	IMocksControl mocksControl = null;
	ExecutorService executorService = null;
	MessageServiceImpl impl = null;
	MessageHandler messageHandler = null;

	@Before
	public void setUp() throws Exception {
		mocksControl = createControl();
		executorService = mocksControl.createMock(ExecutorService.class);
		messageHandler = mocksControl.createMock(MessageHandler.class);
		impl = new MessageServiceImpl(messageHandler, executorService);
	}

	@Test
	public void testCommit() {
		Message msg = MessageSenderImplTest.buildMessage("aa@bb");
		Capture<Runnable> commandCapture = new Capture<Runnable>();
		executorService.execute(capture(commandCapture));
		expectLastCall().once();

		messageHandler.onMessage(msg);
		expectLastCall().once();

		mocksControl.replay();

		impl.commit(msg);

		assertTrue(commandCapture.hasCaptured());
		commandCapture.getValue().run();
	}

}
