package com.babeeta.butterfly.rpc;

import com.babeeta.butterfly.MessageHandler;
import com.babeeta.butterfly.MessageRouting.Message;

/**
 * 远程调用返回处理接口
 * 
 * @author leon
 * 
 */
public interface RPCHandler extends MessageHandler {
	void exceptionCaught(Message message, Throwable t);
}
