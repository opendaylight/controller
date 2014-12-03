/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.scheduledthreadpool;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.Closeable;
import javax.annotation.Nullable;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.RuntimeBeanRegistratorAwareModule;
import org.opendaylight.controller.config.api.runtime.RootRuntimeBeanRegistrator;
import org.opendaylight.controller.config.manager.testingservices.seviceinterface.TestingScheduledThreadPoolServiceInterface;
import org.opendaylight.controller.config.spi.Module;

/**
 * This class has two exported interfaces and two runtime beans. Recreation is
 * triggered by setting Recreate attribute to true.
 */
public class TestingScheduledThreadPoolModule implements Module,
        TestingScheduledThreadPoolConfigBeanMXBean,
        RuntimeBeanRegistratorAwareModule,
        TestingScheduledThreadPoolServiceInterface {

    private final ModuleIdentifier identifier;
    @Nullable
    private final AutoCloseable oldCloseable;
    @Nullable
    private final TestingScheduledThreadPoolImpl oldInstance;

    private int threadCount = 10;
    private TestingScheduledThreadPoolImpl instance;
    private RootRuntimeBeanRegistrator runtimeBeanRegistrator;
    private boolean recreate;

    public TestingScheduledThreadPoolModule(ModuleIdentifier identifier,
            @Nullable AutoCloseable oldCloseable,
            @Nullable TestingScheduledThreadPoolImpl oldInstance) {
        this.identifier = identifier;
        this.oldCloseable = oldCloseable;
        this.oldInstance = oldInstance;
    }

    @Override
    public void setRuntimeBeanRegistrator(
            RootRuntimeBeanRegistrator runtimeBeanRegistrator) {
        this.runtimeBeanRegistrator = runtimeBeanRegistrator;
    }

    @Override
    public void validate() {
        assertNull(runtimeBeanRegistrator);
        // check thread count
        checkState(threadCount > 0,
                "Parameter 'ThreadCount' must be greater than 0");
    }

    @Override
    public boolean canReuse(final Module oldModule) {
        return false;
    }

    @Override
    public int getThreadCount() {
        return threadCount;
    }

    @Override
    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    @Override
    public Closeable getInstance() {
        assertNotNull(runtimeBeanRegistrator);
        if (instance == null) {
            if (oldInstance != null && recreate == false) {
                // reuse old instance
                instance = oldInstance;
            }
            if (instance == null) {
                if (oldCloseable != null) {
                    try {
                        oldCloseable.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                // close old threadpool and esp. unregister runtime beans
                instance = new TestingScheduledThreadPoolImpl(
                        runtimeBeanRegistrator, threadCount);
            }
        }
        return instance;
    }

    // getters and setters
    @Override
    public boolean isRecreate() {
        return recreate;
    }

    @Override
    public void setRecreate(boolean recreate) {
        this.recreate = recreate;
    }

    @Override
    public ModuleIdentifier getIdentifier() {
        return identifier;
    }


}
