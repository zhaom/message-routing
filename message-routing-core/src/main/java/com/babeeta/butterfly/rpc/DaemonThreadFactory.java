package com.babeeta.butterfly.rpc;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

class DaemonThreadFactory implements ThreadFactory {
	private AtomicInteger counter = new AtomicInteger(0);

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, "timeout-cleaner-"
				+ counter.getAndIncrement());
		t.setDaemon(true);
		t.setPriority(Thread.NORM_PRIORITY);
		return t;
	}
}