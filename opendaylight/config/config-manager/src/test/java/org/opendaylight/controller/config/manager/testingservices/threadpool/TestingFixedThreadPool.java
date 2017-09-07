/*
 * Copyright (c) 2013, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.threadpool;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Lists;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class TestingFixedThreadPool implements TestingThreadPoolIfc, Closeable,
        TestingModifiableThreadPoolIfc {
    private final ThreadPoolExecutor executorService;
    private final String uniqueName;

    public static void cleanUp() {
        for (ExecutorService executorService : ALL_EXECUTORS) {
            executorService.shutdown();
        }
        ALL_EXECUTORS.clear();
    }

    // for verification purposes:
    public static final List<ThreadPoolExecutor> ALL_EXECUTORS = Collections
            .synchronizedList(Lists.<ThreadPoolExecutor>newLinkedList());

    public TestingFixedThreadPool(final int threadCount, final String uniqueName) {
        checkNotNull(uniqueName);
        this.uniqueName = uniqueName;
        executorService = (ThreadPoolExecutor) Executors
                .newFixedThreadPool(threadCount);
        ALL_EXECUTORS.add(executorService);
    }

    @Override
    public Executor getExecutor() {
        return executorService;
    }

    @Override
    public void close() throws IOException {
        executorService.shutdown();
        ALL_EXECUTORS.remove(executorService);

    }

    @Override
    public int getMaxNumberOfThreads() {
        return executorService.getMaximumPoolSize();
    }

    public String getUniqueName() {
        return uniqueName;
    }

    @Override
    public void setMaximumNumberOfThreads(final int activeCount) {
        checkArgument(activeCount > 0);
        executorService.setMaximumPoolSize(activeCount);
    }
}
