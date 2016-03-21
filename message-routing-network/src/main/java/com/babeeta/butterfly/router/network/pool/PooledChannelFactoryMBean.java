package com.babeeta.butterfly.router.network.pool;

public interface PooledChannelFactoryMBean {

	int getMaxActive();

	int getMaxIdle();

	int getNumActive();

	int getNumActive(String host);

	int getNumIdle();

	boolean getTestOnBorrow();

	boolean getTestOnReturn();

	long getTimeBetweenEvictionRunsMillis();

	void setMaxActive(int maxActive);

	void setMaxIdle(int maxIdle);

	void setMaxTotal(int maxTotal);

	void setTestOnBorrow(boolean on);

	void setTestOnReturn(boolean on);

	void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis);
}
