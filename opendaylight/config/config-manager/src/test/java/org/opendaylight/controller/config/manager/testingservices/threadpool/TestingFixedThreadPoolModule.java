/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.threadpool;

import static com.google.common.base.Preconditions.checkState;

import java.io.Closeable;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.manager.testingservices.seviceinterface.ModifiableThreadPoolServiceInterface;
import org.opendaylight.controller.config.spi.Module;

@NotThreadSafe
public class TestingFixedThreadPoolModule implements
        TestingFixedThreadPoolConfigMXBean, Module,
        TestingThreadPoolConfigMXBean, ModifiableThreadPoolServiceInterface {
    private final AutoCloseable oldCloseable;
    private final TestingFixedThreadPool oldInstance;
    private final ModuleIdentifier name;
    private TestingFixedThreadPool instance;
    private int threadCount = 0;
    private boolean triggerNewInstanceCreation;

    TestingFixedThreadPoolModule(ModuleIdentifier name,
            @Nullable AutoCloseable oldCloseable,
            @Nullable TestingFixedThreadPool oldInstance) {
        this.name = name;
        this.oldCloseable = oldCloseable;
        this.oldInstance = oldInstance;
    }


    // attributes
    @Override
    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    @Override
    public int getThreadCount() {
        return threadCount;
    }

    @Override
    public boolean isTriggerNewInstanceCreation() {
        return triggerNewInstanceCreation;
    }

    @Override
    public void setTriggerNewInstanceCreation(boolean triggerNewInstanceCreation) {
        this.triggerNewInstanceCreation = triggerNewInstanceCreation;
    }

    // operations

    private boolean isReusable() {
        return oldInstance != null;
    }

    @Override
    public void validate() {
        checkState(threadCount > 0,
                "Parameter 'threadCount' must be greater than 0");
    }

    @Override
    public boolean canReuse(final Module oldModule) {
        return isReusable() && triggerNewInstanceCreation == false;
    }

    @Override
    public Closeable getInstance() {
        if (instance == null) {
            if (isReusable() && triggerNewInstanceCreation == false) { // simulate
                                                                       // big
                                                                       // change
                                                                       // using
                                                                       // triggerNewInstanceCreation
                oldInstance.setMaximumNumberOfThreads(threadCount);
                instance = oldInstance;
            } else {
                if (oldCloseable != null) {
                    try {
                        oldCloseable.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                instance = new TestingFixedThreadPool(threadCount,
                        name.toString());
            }
        }
        return instance;
    }

    @Override
    public ModuleIdentifier getIdentifier() {
        return name;
    }

}
