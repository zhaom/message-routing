package com.babeeta.butterfly.rpc;

public interface MessageBasedRPCServiceMBean {

	/**
	 * 取累计的调用次数
	 * 
	 * @return
	 */
	public abstract long getCounter();

	/**
	 * 查询当前正在进行的调用数量
	 * 
	 * @return
	 */
	public abstract int getPendingInvokeCount();

}