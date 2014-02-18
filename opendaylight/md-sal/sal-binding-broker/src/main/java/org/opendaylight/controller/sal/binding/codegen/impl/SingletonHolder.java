/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javassist.ClassPool;

import org.opendaylight.controller.sal.binding.codegen.RuntimeCodeGenerator;
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

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
            ThreadFactory factory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("md-sal-binding-notification-%d").build();
            ExecutorService executor = new ThreadPoolExecutor(CORE_NOTIFICATION_THREADS, MAX_NOTIFICATION_THREADS,
                    NOTIFICATION_THREAD_LIFE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), factory);
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
