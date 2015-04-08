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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javassist.ClassPool;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingletonHolder {
    private static final Logger LOG = LoggerFactory.getLogger(SingletonHolder.class);

    public static final ClassPool CLASS_POOL = ClassPool.getDefault();
    public static final JavassistUtils JAVASSIST = JavassistUtils.forClassPool(CLASS_POOL);

    public static final int CORE_NOTIFICATION_THREADS = 4;
    public static final int MAX_NOTIFICATION_THREADS = 32;
    // block caller thread after MAX_NOTIFICATION_THREADS + MAX_NOTIFICATION_QUEUE_SIZE pending notifications
    public static final int MAX_NOTIFICATION_QUEUE_SIZE = 1000;
    public static final int NOTIFICATION_THREAD_LIFE = 15;

    private static ListeningExecutorService COMMIT_EXECUTOR = null;
    private static ListeningExecutorService CHANGE_EVENT_EXECUTOR = null;

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
}
