/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen.impl;

import com.google.common.util.concurrent.ForwardingBlockingQueue;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javassist.ClassPool;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingletonHolder {
    private static final Logger logger = LoggerFactory.getLogger(SingletonHolder.class);

    public static final ClassPool CLASS_POOL = ClassPool.getDefault();
    public static final JavassistUtils JAVASSIST = JavassistUtils.forClassPool(CLASS_POOL);

    public static final int CORE_NOTIFICATION_THREADS = 4;
    public static final int MAX_NOTIFICATION_THREADS = 32;
    // block caller thread after MAX_NOTIFICATION_THREADS + MAX_NOTIFICATION_QUEUE_SIZE pending notifications
    public static final int MAX_NOTIFICATION_QUEUE_SIZE = 1000;
    public static final int NOTIFICATION_THREAD_LIFE = 15;
    private static final String NOTIFICATION_QUEUE_SIZE_PROPERTY = "mdsal.notificationqueue.size";

    private static ListeningExecutorService NOTIFICATION_EXECUTOR = null;
    private static ListeningExecutorService COMMIT_EXECUTOR = null;
    private static ListeningExecutorService CHANGE_EVENT_EXECUTOR = null;

    /**
     * @deprecated This method is only used from configuration modules and thus callers of it
     *             should use service injection to make the executor configurable.
     */
    @Deprecated
    public static synchronized ListeningExecutorService getDefaultNotificationExecutor() {

        if (NOTIFICATION_EXECUTOR == null) {
            int queueSize = MAX_NOTIFICATION_QUEUE_SIZE;
            final String queueValue = System.getProperty(NOTIFICATION_QUEUE_SIZE_PROPERTY);
            if (StringUtils.isNotBlank(queueValue)) {
                try {
                    queueSize = Integer.parseInt(queueValue);
                    logger.trace("Queue size was set to {}", queueSize);
                } catch (final NumberFormatException e) {
                    logger.warn("Cannot parse {} as set by {}, using default {}", queueValue,
                            NOTIFICATION_QUEUE_SIZE_PROPERTY, queueSize);
                }
            }

            // Overriding the queue:
            // ThreadPoolExecutor would not create new threads if the queue is not full, thus adding
            // occurs in RejectedExecutionHandler.
            // This impl saturates threadpool first, then queue. When both are full caller will get blocked.
            final BlockingQueue<Runnable> delegate = new LinkedBlockingQueue<>(queueSize);
            final BlockingQueue<Runnable> queue = new ForwardingBlockingQueue<Runnable>() {
                @Override
                protected BlockingQueue<Runnable> delegate() {
                    return delegate;
                }

                @Override
                public boolean offer(final Runnable r) {
                    // ThreadPoolExecutor will spawn a new thread after core size is reached only
                    // if the queue.offer returns false.
                    return false;
                }
            };

            final ThreadFactory factory = new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("md-sal-binding-notification-%d")
            .build();

            final ThreadPoolExecutor executor = new ThreadPoolExecutor(CORE_NOTIFICATION_THREADS, MAX_NOTIFICATION_THREADS,
                    NOTIFICATION_THREAD_LIFE, TimeUnit.SECONDS, queue, factory,
                    new RejectedExecutionHandler() {
                // if the max threads are met, then it will raise a rejectedExecution. We then push to the queue.
                @Override
                public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
                    try {
                        executor.getQueue().put(r);
                    } catch (final InterruptedException e) {
                        throw new RejectedExecutionException("Interrupted while waiting on the queue", e);
                    }
                }
            });

            NOTIFICATION_EXECUTOR = MoreExecutors.listeningDecorator(executor);
        }

        return NOTIFICATION_EXECUTOR;
    }

    /**
     * @deprecated This method is only used from configuration modules and thus callers of it
     *             should use service injection to make the executor configurable.
     */
    @Deprecated
    public static synchronized ListeningExecutorService getDefaultCommitExecutor() {
        if (COMMIT_EXECUTOR == null) {
            final ThreadFactory factory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("md-sal-binding-commit-%d").build();
            /*
             * FIXME: this used to be newCacheThreadPool(), but MD-SAL does not have transaction
             *        ordering guarantees, which means that using a concurrent threadpool results
             *        in application data being committed in random order, potentially resulting
             *        in inconsistent data being present. Once proper primitives are introduced,
             *        concurrency can be reintroduced.
             */
            final ExecutorService executor = Executors.newSingleThreadExecutor(factory);
            COMMIT_EXECUTOR = MoreExecutors.listeningDecorator(executor);
        }

        return COMMIT_EXECUTOR;
    }

    public static ExecutorService getDefaultChangeEventExecutor() {
        if (CHANGE_EVENT_EXECUTOR == null) {
            final ThreadFactory factory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("md-sal-binding-change-%d").build();
            /*
             * FIXME: this used to be newCacheThreadPool(), but MD-SAL does not have transaction
             *        ordering guarantees, which means that using a concurrent threadpool results
             *        in application data being committed in random order, potentially resulting
             *        in inconsistent data being present. Once proper primitives are introduced,
             *        concurrency can be reintroduced.
             */
            final ExecutorService executor = Executors.newSingleThreadExecutor(factory);
            CHANGE_EVENT_EXECUTOR  = MoreExecutors.listeningDecorator(executor);
        }

        return CHANGE_EVENT_EXECUTOR;
    }
}
