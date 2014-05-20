package com.kraken.mediasend.gallery;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolUtil
{

	private static ExecutorService executorService;

	private static int maxThreadNum = 40;

	private static void initService()
	{
		if (executorService == null)
		{
			executorService = Executors.newFixedThreadPool(maxThreadNum);
		}
	}

	/**
	 * @Description:执行
	 * @Author: wanghb
	 * @Email: wanghb@foryouge.com.cn
	 * @param runnable
	 * @Others:
	 */
	public static void addRunnable(final Runnable runnable)
	{
		initService();
		executorService.execute(new Runnable()
		{
			public void run()
			{
				try
				{
					runnable.run();
				} catch (Throwable e)
				{
					e.printStackTrace();
				}
			}
		});
	}

}
