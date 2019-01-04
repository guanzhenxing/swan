package cn.webfuse.framework.core.kit.concurrent.threadpool;

import cn.webfuse.framework.core.kit.concurrent.JstackKits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * copy from vipshop VJTools
 * 移植自Dubbo.
 * <p>
 * Abort Policy.
 * Log warn info when abort.
 */
public class AbortPolicyWithReport extends ThreadPoolExecutor.AbortPolicy {

    protected static final Logger logger = LoggerFactory.getLogger(AbortPolicyWithReport.class);

    private final String threadName;

    private static volatile long lastPrintTime = 0;

    private static Semaphore guard = new Semaphore(1);

    public AbortPolicyWithReport(String threadName) {
        this.threadName = threadName;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        String msg = String.format("Thread pool is EXHAUSTED!" +
                        " Thread Name: %s, Pool Size: %d (active: %d, core: %d, max: %d, largest: %d), Task: %d (completed: %d)," +
                        " Executor status:(isShutdown:%s, isTerminated:%s, isTerminating:%s)!",
                threadName, e.getPoolSize(), e.getActiveCount(), e.getCorePoolSize(), e.getMaximumPoolSize(), e.getLargestPoolSize(),
                e.getTaskCount(), e.getCompletedTaskCount(), e.isShutdown(), e.isTerminated(), e.isTerminating());
        logger.warn(msg);
        dumpJStack();
        throw new RejectedExecutionException(msg);
    }

    private void dumpJStack() {
        long now = System.currentTimeMillis();

        //dump every 10 minutes
        if (now - lastPrintTime < 10 * 60 * 1000) {
            return;
        }

        if (!guard.tryAcquire()) {
            return;
        }
        Executors.newSingleThreadExecutor().execute(() -> {
            String dumpPath = System.getProperty("user.home");

            SimpleDateFormat sdf;

            String OS = System.getProperty("os.name").toLowerCase();

            // window system don't context ":" in file name
            if (OS.contains("win")) {
                sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            } else {
                sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
            }

            String dateStr = sdf.format(new Date());
            FileOutputStream jstackStream = null;
            try {
                jstackStream = new FileOutputStream(new File(dumpPath, threadName + "_JStack.log" + "." + dateStr));
                JstackKits.jstack(jstackStream);

            } catch (Throwable t) {
                logger.error("dump jstack error", t);
            } finally {
                guard.release();
                if (jstackStream != null) {
                    try {
                        jstackStream.flush();
                        jstackStream.close();
                    } catch (IOException e) {
                    }
                }
            }

            lastPrintTime = System.currentTimeMillis();
        });

    }

}
