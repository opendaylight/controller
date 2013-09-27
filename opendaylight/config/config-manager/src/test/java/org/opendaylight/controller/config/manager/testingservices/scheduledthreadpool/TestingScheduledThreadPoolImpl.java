/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.scheduledthreadpool;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.opendaylight.controller.config.api.runtime.HierarchicalRuntimeBeanRegistration;
import org.opendaylight.controller.config.api.runtime.RootRuntimeBeanRegistrator;
import org.opendaylight.controller.config.manager.testingservices.scheduledthreadpool.runtimebeans
        .TestingScheduledRuntimeBean;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingThreadPoolIfc;

import com.google.common.collect.Lists;

public class TestingScheduledThreadPoolImpl implements TestingThreadPoolIfc,
        TestingScheduledThreadPoolIfc, Closeable {
    private static volatile int numberOfCloseMethodCalls = 0;
    private final ScheduledThreadPoolExecutor executor;
    private final RootRuntimeBeanRegistrator runtimeBeanRegistrator;

    public static final List<ScheduledThreadPoolExecutor> allExecutors = Lists
            .newLinkedList();

    public TestingScheduledThreadPoolImpl(
            RootRuntimeBeanRegistrator runtimeBeanRegistrator, int corePoolSize) {
        this.runtimeBeanRegistrator = runtimeBeanRegistrator;
        executor = new ScheduledThreadPoolExecutor(corePoolSize);
        allExecutors.add(executor);
        HierarchicalRuntimeBeanRegistration hierarchicalRuntimeBeanRegistration = runtimeBeanRegistrator
                .registerRoot(new TestingScheduledRuntimeBean());
        hierarchicalRuntimeBeanRegistration.register("a", "b",
                new TestingScheduledRuntimeBean());
    }

    @Override
    public void close() {
        numberOfCloseMethodCalls++;
        runtimeBeanRegistrator.close();
        executor.shutdown();
    }

    @Override
    public ScheduledExecutorService getScheduledExecutor() {
        return executor;
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    @Override
    public int getMaxNumberOfThreads() {
        return executor.getCorePoolSize();
    }

    public static void cleanUp() {
        for (ScheduledThreadPoolExecutor executor : allExecutors) {
            executor.shutdown();
        }
        allExecutors.clear();
        numberOfCloseMethodCalls = 0;
    }

    public static int getNumberOfCloseMethodCalls() {
        return numberOfCloseMethodCalls;
    }

}
