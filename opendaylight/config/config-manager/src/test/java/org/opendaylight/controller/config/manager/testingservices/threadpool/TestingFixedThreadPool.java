/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.threadpool;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.common.collect.Lists;

public class TestingFixedThreadPool implements TestingThreadPoolIfc, Closeable,
        TestingModifiableThreadPoolIfc {
    private final ThreadPoolExecutor executorService;
    private final String uniqueName;

    public static void cleanUp() {
        for (ExecutorService executorService : allExecutors) {
            executorService.shutdown();
        }
        allExecutors.clear();
    }

    // for verification purposes:
    public static final List<ThreadPoolExecutor> allExecutors = Collections
            .synchronizedList(Lists.<ThreadPoolExecutor>newLinkedList());

    public TestingFixedThreadPool(int threadCount, String uniqueName) {
        checkNotNull(uniqueName);
        this.uniqueName = uniqueName;
        executorService = (ThreadPoolExecutor) Executors
                .newFixedThreadPool(threadCount);
        allExecutors.add(executorService);
    }

    @Override
    public Executor getExecutor() {
        return executorService;
    }

    @Override
    public void close() throws IOException {
        executorService.shutdown();

    }

    @Override
    public int getMaxNumberOfThreads() {
        return executorService.getMaximumPoolSize();
    }

    public String getUniqueName() {
        return uniqueName;
    }

    @Override
    public void setMaximumNumberOfThreads(int activeCount) {
        checkArgument(activeCount > 0);
        executorService.setMaximumPoolSize(activeCount);
    }

}
