package com.babeeta.butterfly.rpc;

import com.babeeta.butterfly.MessageRouting.Message;

/**
 * 远程异步调用服务
 * 
 * @author leon
 * 
 */
public interface RPCService {
	void invoke(Message message, RPCHandler responseHandler);
}
