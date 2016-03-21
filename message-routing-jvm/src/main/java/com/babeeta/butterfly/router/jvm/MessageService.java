package com.babeeta.butterfly.router.jvm;

import com.babeeta.butterfly.MessageRouting.Message;

/**
 * 消息处理接口
 * 
 * @author leon
 * 
 */
public interface MessageService {
	/**
	 * 提交一个达到的消息
	 * 
	 * @param message
	 */
	void commit(final Message message);
}
