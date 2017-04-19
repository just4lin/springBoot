package just.async;

import java.util.concurrent.Callable;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.CustomizableThreadCreator;

/**
 * 
 * Filename:    DelayAsyncTaskExecutor.java  
 * Description: 延时启动异步线程执行器
 * 				有时会存在一种场景，当前业务端事务尚未提交，但异步处理已经根据业务id查询相关业务数据
 * 				这时我们希望这个异步处理晚些时间去执行  
 * Copyright:   Copyright (c) 2012-2013 All Rights Reserved.
 * Company:     golden-soft.com Inc.
 * @author:     li 
 * @version:    1.0  
 * Create at:   2017年4月18日 下午3:37:39  
 *  
 * Modification History:  
 * Date         Author      Version     Description  
 * ------------------------------------------------------------------  
 * 2017年4月18日      li      1.0         1.0 Version  
 *
 */
@Service("delayAsyncTaskExecutor")
public class DelayAsyncTaskExecutor extends CustomizableThreadCreator implements AsyncTaskExecutor,InitializingBean,DisposableBean{

	private static final long serialVersionUID = 1L;
	
	private static final Logger logger = LoggerFactory.getLogger(DelayAsyncTaskExecutor.class);
	
	private ThreadFactory threadFactory;
	//无界阻塞延迟队列
	private DelayQueue<DelayTask> delayQueue = new DelayQueue<DelayTask>();
	//默认延迟执行时间
	private long defaultDelayTime = 3;
	//线程执行器
	private ExecutorService executorService;

	@Override
	public void execute(Runnable task) {
		execute(task, defaultDelayTime);
	}

	@Override
	public void execute(Runnable task, long startTimeout) {
		logger.debug("#submit# 提交任务至线程池，延迟执行时间为:{}",startTimeout);
		DelayTask delayTask = new DelayTask(task,startTimeout);
		delayQueue.offer(delayTask);
	}

	@Override
	public Future<?> submit(Runnable task) {
		FutureTask<Object> future = new FutureTask<Object>(task, null);
		execute(future, defaultDelayTime);
		return future;
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		FutureTask<T> future = new FutureTask<T>(task);
		execute(future, defaultDelayTime);
		return future;
	}
	
	/**
	 * Template method for the actual execution of a task.
	 * <p>The default implementation creates a new Thread and starts it.
	 * @param task the Runnable to execute
	 * @see #setThreadFactory
	 * @see #createThread
	 * @see java.lang.Thread#start()
	 */
	protected void doExecute(Runnable task) {
		Thread thread = (this.threadFactory != null ? this.threadFactory.newThread(task) : createThread(task));
		thread.start();
	}
	
	public class DelayQueueManage implements Runnable{
		@Override
		public void run() {
			while(true){
				try {
					logger.debug("#DelayQueueManage-run# 开始执行异步队列监控 ...wait...");
					DelayTask take = delayQueue.take();
					logger.debug("#DelayQueueManage-run# 已获取待执行队列开始执行任务 ...start...");
					doExecute(take.getTask());
					logger.debug("#DelayQueueManage-run# 异步任务已提交执行 ...end...");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public class DelayTask implements Delayed{

		private Runnable task;
		
		private Long initNanoTime;
		
		private Long startNanoTime;
		
		private Long delayTime;
		
		public DelayTask(Runnable task,Long delayTime){
			this.task = task;
			this.initNanoTime = System.nanoTime();
			this.delayTime = delayTime;
			this.startNanoTime = initNanoTime + TimeUnit.SECONDS.toNanos(delayTime);
		}
		
		@Override
		public int compareTo(Delayed o) {
			if(o == null || ! (o instanceof DelayTask)) 
				return 1;
		    if(o == this) 
		    	return 0; 
		    DelayTask dt = (DelayTask)o;
		    if (this.initNanoTime > dt.initNanoTime) {
		        return 1;
		    }else if (this.initNanoTime == dt.initNanoTime) {
		        return 0;
		    }else {
		        return -1;
		    }
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(startNanoTime - System.nanoTime(),  TimeUnit.NANOSECONDS);
		}

		public Runnable getTask() {
			return task;
		}

		public Long getDelayTime() {
			return delayTime;
		}

	}

	@Override
	public void afterPropertiesSet() throws Exception {
		executorService = Executors.newSingleThreadExecutor();
		executorService.execute(new DelayQueueManage());
	}

	@Override
	public void destroy() throws Exception {
		if(executorService != null && !executorService.isShutdown()){
			int size = delayQueue.size();
			if(size > 0){
				logger.error("#destroy# 延迟任务队列中仍存在"+size+"个任务未执行!");
			}
			executorService.shutdown();
		}
	}


}
