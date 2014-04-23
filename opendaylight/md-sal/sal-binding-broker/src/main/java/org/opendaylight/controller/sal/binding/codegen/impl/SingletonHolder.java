/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen.impl;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import javassist.ClassPool;
import org.opendaylight.controller.sal.binding.codegen.RuntimeCodeGenerator;
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SingletonHolder {

    public static final ClassPool CLASS_POOL = ClassPool.getDefault();
    public static final org.opendaylight.controller.sal.binding.codegen.impl.RuntimeCodeGenerator RPC_GENERATOR_IMPL = new org.opendaylight.controller.sal.binding.codegen.impl.RuntimeCodeGenerator(
            CLASS_POOL);
    public static final RuntimeCodeGenerator RPC_GENERATOR = RPC_GENERATOR_IMPL;
    public static final NotificationInvokerFactory INVOKER_FACTORY = RPC_GENERATOR_IMPL.getInvokerFactory();

    public static final int CORE_NOTIFICATION_THREADS = 4;
    public static final int MAX_NOTIFICATION_THREADS = 32;
    public static final int NOTIFICATION_THREAD_LIFE = 15;

    private static ListeningExecutorService NOTIFICATION_EXECUTOR = null;
    private static ListeningExecutorService COMMIT_EXECUTOR = null;
    private static ListeningExecutorService CHANGE_EVENT_EXECUTOR = null;

    /**
     * @deprecated This method is only used from configuration modules and thus callers of it
     *             should use service injection to make the executor configurable.
     */
    @Deprecated
    public static synchronized final ListeningExecutorService getDefaultNotificationExecutor() {

        if (NOTIFICATION_EXECUTOR == null) {
            // Overriding the queue since we need an unbounded queue
            // and threadpoolexecutor would not create new threads if the queue is not full
            BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>() {
                @Override
                public boolean offer(Runnable r) {
                    if (size() <= 1) {
                        // if the queue is empty (or has just 1), no need to rampup the threads
                        return super.offer(r);
                    } else {
                        // if the queue is not empty, force the queue to return false.
                        // threadpoolexecutor will spawn a new thread if the queue.offer returns false.
                        return false;
                    }
                }
            };

            ThreadFactory factory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("md-sal-binding-notification-%d").build();

            ThreadPoolExecutor executor = new ThreadPoolExecutor(CORE_NOTIFICATION_THREADS, MAX_NOTIFICATION_THREADS,
                    NOTIFICATION_THREAD_LIFE, TimeUnit.SECONDS, queue , factory,
                    new RejectedExecutionHandler() {
                        // if the max threads are met, then it will raise a rejectedExecution. We then push to the queue.
                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                            try {
                                executor.getQueue().put(r);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
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
    public static synchronized final ListeningExecutorService getDefaultCommitExecutor() {
        if (COMMIT_EXECUTOR == null) {
            ThreadFactory factory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("md-sal-binding-commit-%d").build();
            /*
             * FIXME: this used to be newCacheThreadPool(), but MD-SAL does not have transaction
             *        ordering guarantees, which means that using a concurrent threadpool results
             *        in application data being committed in random order, potentially resulting
             *        in inconsistent data being present. Once proper primitives are introduced,
             *        concurrency can be reintroduced.
             */
            ExecutorService executor = Executors.newSingleThreadExecutor(factory);
            COMMIT_EXECUTOR = MoreExecutors.listeningDecorator(executor);
        }

        return COMMIT_EXECUTOR;
    }

    public static ExecutorService getDefaultChangeEventExecutor() {
        if (CHANGE_EVENT_EXECUTOR == null) {
            ThreadFactory factory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("md-sal-binding-change-%d").build();
            /*
             * FIXME: this used to be newCacheThreadPool(), but MD-SAL does not have transaction
             *        ordering guarantees, which means that using a concurrent threadpool results
             *        in application data being committed in random order, potentially resulting
             *        in inconsistent data being present. Once proper primitives are introduced,
             *        concurrency can be reintroduced.
             */
            ExecutorService executor = Executors.newSingleThreadExecutor(factory);
            CHANGE_EVENT_EXECUTOR  = MoreExecutors.listeningDecorator(executor);
        }

        return CHANGE_EVENT_EXECUTOR;
    }
}
