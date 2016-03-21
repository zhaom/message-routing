package com.babeeta.butterfly.router.network.monitor;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA. User: XYuser Date: 10-12-23 Time: 下午7:15 To change
 * this template use File | Settings | File Templates.
 */
public interface NetworkMBean {
	void beginDump(String file) throws IOException;

	int getChannelGroupSize();

	void stopDump() throws IOException;

	int getThreadPoolActiveCount();

	int getThreadPoolMaxPoolSize();

	int getThreadPoolQueueLength();

    void resetThreadCorePoolSize(int coreSize);

    void resetThreadMaximumPoolSize(int maximumSize);
}
