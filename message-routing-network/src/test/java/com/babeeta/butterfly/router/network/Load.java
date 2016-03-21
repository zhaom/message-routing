package com.babeeta.butterfly.router.network;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.babeeta.butterfly.MessageRouting.Message;
import com.google.protobuf.ByteString;

public class Load {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ExecutorService e = Executors.newCachedThreadPool();
		final MessageSenderImpl impl = new MessageSenderImpl();

		final Message msg = Message.newBuilder().setUid("UID")
				.setFrom("aa@test.dev")
				.setTo("auth@accounts.dev")
				.setContent(ByteString.copyFrom("Hello".getBytes()))
				.setDate(System.currentTimeMillis())
				.build();

		for (int i = 0; i < 100; i++) {
			e.execute(new Runnable() {

				@Override
				public void run() {
					while (true) {
						impl.send(msg);
						try {
							Thread.sleep(10L);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			});
		}

	}
}
