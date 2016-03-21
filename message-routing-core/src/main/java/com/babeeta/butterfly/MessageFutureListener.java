package com.babeeta.butterfly;

/**
 * 操作异步处理结果监听器
 * 
 * @author leon
 * 
 */
public interface MessageFutureListener {
	void operationComplete(MessageFuture future);
}
