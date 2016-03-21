package com.babeeta.butterfly;

/**
 * <p>
 * 消息异步发送报告
 * </p>
 * <p>
 * <strong>注意:永远不要在MessageFutureListener中调用await方法</strong>
 * </p>
 * 
 * @author leon
 * 
 */
public interface MessageFuture {
	/**
	 * <p>
	 * 注册当操作完成时的监听器。
	 * </p>
	 * <p>
	 * 如果当前操作已经完成， 则监听器会立即在当前线程中调用。否则， 监听器会在Worker线程中被调用
	 * </p>
	 * 
	 * @param listener
	 * @return 当前的MessageFuture对象。
	 */
	MessageFuture addListener(MessageFutureListener listener);

	/**
	 * <p>
	 * 阻塞当前线程，等候发送完成。
	 * </p>
	 * <p>
	 * <strong>注意:永远不要在MessageFutureListener中调用await方法</strong>
	 * </p>
	 * 
	 * @return 当前的MessageFuture对象
	 * @throws InterruptedException
	 */
	MessageFuture await() throws InterruptedException;

	/**
	 * <p>
	 * 在指定时间内阻塞当前线程
	 * </p>
	 * <p>
	 * <strong>注意:永远不要在MessageFutureListener中调用await方法</strong>
	 * </p>
	 * 
	 * @param timeoutMillins
	 *            最长等待时间
	 * @return 操作是否完成
	 * @throws InterruptedException
	 */
	boolean await(long timeoutMillins) throws InterruptedException;

	/**
	 * 操作失败原因
	 * 
	 * @return 底层系统抛出的异常
	 */
	Throwable getCause();

	/**
	 * 操作是否完成
	 * 
	 * @return true， 操作完成（无论是否成功）
	 */
	boolean isDone();

	/**
	 * 操作是否成功
	 * 
	 * @return true， 操作完成并且成功； false，操作完成但是发生了错误。可以使用getCause()方法得到Exception对象
	 */
	boolean isSuccess();

	/**
	 * 删除监听器
	 * 
	 * @param listener
	 * @return <tt>true</tt> Listener集合中包含这个Listener
	 */
	boolean removeListener(MessageFutureListener listener);
}
