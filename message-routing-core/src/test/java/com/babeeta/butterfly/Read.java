package com.babeeta.butterfly;

import java.io.FileOutputStream;
import java.util.UUID;

import com.babeeta.butterfly.MessageRouting.HeartbeatInit;
import com.babeeta.butterfly.MessageRouting.Message;
import com.babeeta.butterfly.MessageRouting.ServiceBind;
import com.google.protobuf.ByteString;

public class Read {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Message msg = Message.newBuilder()
				.setContent(ByteString.copyFromUtf8("Hello, there!"))
				.setDate(System.currentTimeMillis())
				.setFrom("FROM")
				.setTo("TO")
				.setUid(UUID.randomUUID().toString().replaceAll("\\-", ""))
				.addVia("a")
				.addVia("b")
				.addVia("c")
				.addVia("d")
				.build();

		FileOutputStream out = new FileOutputStream(
				"/home/leon/message.dat");
		out.write(msg.toByteArray());
		out.close();

		ServiceBind serviceBind = ServiceBind.newBuilder()
				.setApplicationId("AID")
				.build();

		out = new FileOutputStream(
				"/home/leon/service_bind.dat");
		out.write(serviceBind.toByteArray());
		out.close();

		msg = msg.toBuilder().clearVia().build();

		out = new FileOutputStream(
				"/home/leon/message_without_via.dat");
		out.write(msg.toByteArray());
		out.close();

		HeartbeatInit heatbeatInit = HeartbeatInit
				.newBuilder()
				.setCause("CAuse")
				.setLastTimeout(120)
				.setLastException(
						com.babeeta.butterfly.MessageRouting.HeartbeatInit.HeartbeatException.choke)
				.build();

		out = new FileOutputStream(
				"/home/leon/heart_beat.dat");
		out.write(heatbeatInit.toByteArray());
		out.close();
	}

}
