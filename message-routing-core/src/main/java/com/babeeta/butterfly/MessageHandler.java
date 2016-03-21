package com.babeeta.butterfly;

/**
 * 消息监听接口
 * @author leon
 *
 */
public interface MessageHandler {
	void onMessage(MessageRouting.Message message);
}
