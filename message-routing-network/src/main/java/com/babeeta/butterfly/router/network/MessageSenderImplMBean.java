package com.babeeta.butterfly.router.network;

public interface MessageSenderImplMBean {

	int getMessageQueueLength();

	int getThreadPoolSize();

	void setThreadPoolSize(int corePoolSize);

}
