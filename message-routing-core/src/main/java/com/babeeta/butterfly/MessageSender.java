package com.babeeta.butterfly;


/**
 * 消息发送接口
 * @author leon
 *
 */
public interface MessageSender {
	MessageFuture send(MessageRouting.Message message);
}
