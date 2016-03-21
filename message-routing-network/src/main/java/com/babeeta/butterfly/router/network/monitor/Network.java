package com.babeeta.butterfly.router.network.monitor;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.babeeta.butterfly.router.network.DataDumpHandler;
import com.babeeta.butterfly.router.network.RequestHandler;

/**
 * Created by IntelliJ IDEA. User: XYuser Date: 10-12-23 Time: 下午7:15 To change
 * this template use File | Settings | File Templates.
 */
public class Network implements NetworkMBean {

	private static final Logger logger = LoggerFactory.getLogger(Network.class);

	private final DataDumpHandler dataDumpHandler;
	private final ThreadPoolExecutor threadPoolExecutor;
	private static final AtomicInteger counter = new AtomicInteger();

	public Network(DataDumpHandler dataDumpHandler,
			ExecutorService executorService) {
		super();
		this.dataDumpHandler = dataDumpHandler;
		this.threadPoolExecutor = (ThreadPoolExecutor) executorService;
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		try {
			int index = counter.incrementAndGet();
			ObjectName objectName = new ObjectName(
					"com.babeeta.butterfly.router.network.monitor:name=Network-"
							+ (index));
			mBeanServer.registerMBean(this, objectName);
			logger.info("Network-" + index + " MBean Server is started.");
		} catch (Exception e) {
			logger.info("Network MBean Server bootstrap error:" + e);
		}
	}

	@Override
	public void beginDump(String file) throws IOException {
		dataDumpHandler.startDump(file);
	}

	@Override
	public int getChannelGroupSize() {
		return RequestHandler.getChannelGroupSize();
	}

	@Override
	public int getThreadPoolActiveCount() {
		return this.threadPoolExecutor.getActiveCount();
	}

	@Override
	public int getThreadPoolMaxPoolSize() {
		return this.threadPoolExecutor.getMaximumPoolSize();
	}

	@Override
	public int getThreadPoolQueueLength() {
		return this.threadPoolExecutor.getQueue().size();
	}

	@Override
	public void resetThreadCorePoolSize(int size) {
		this.threadPoolExecutor.setCorePoolSize(size);
	}

	@Override
	public void resetThreadMaximumPoolSize(int maximumSize) {
		this.threadPoolExecutor.setMaximumPoolSize(maximumSize);
	}

	@Override
	public void stopDump() throws IOException {
		dataDumpHandler.stopDump();
	}
}