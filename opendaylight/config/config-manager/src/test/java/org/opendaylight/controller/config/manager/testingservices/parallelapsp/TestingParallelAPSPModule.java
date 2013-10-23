/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.parallelapsp;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.Closeable;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.management.ObjectName;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.JmxAttribute;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.annotations.RequireInterface;
import org.opendaylight.controller.config.manager.testingservices.seviceinterface.TestingThreadPoolServiceInterface;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingThreadPoolIfc;
import org.opendaylight.controller.config.spi.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * Represents service that has dependency to thread pool.
 */
@NotThreadSafe
public class TestingParallelAPSPModule implements Module,
        TestingParallelAPSPConfigMXBean {
    private static final Logger logger = LoggerFactory
            .getLogger(TestingParallelAPSPModule.class);

    private final DependencyResolver dependencyResolver;
    private final AutoCloseable oldCloseable;
    private final TestingParallelAPSPImpl oldInstance;
    private final ModuleIdentifier identifier;
    private ObjectName threadPoolON;
    private TestingParallelAPSPImpl instance;
    private String someParam;

    public TestingParallelAPSPModule(ModuleIdentifier identifier,
            DependencyResolver dependencyResolver,
            @Nullable AutoCloseable oldCloseable,
            @Nullable TestingParallelAPSPImpl oldInstance) {
        this.identifier = identifier;
        this.dependencyResolver = dependencyResolver;
        this.oldCloseable = oldCloseable;
        this.oldInstance = oldInstance;
    }

    @Override
    public ObjectName getThreadPool() {
        return threadPoolON;
    }

    @RequireInterface(TestingThreadPoolServiceInterface.class)
    @Override
    public void setThreadPool(ObjectName threadPoolName) {
        this.threadPoolON = threadPoolName;
    }

    @Override
    public String getSomeParam() {
        return someParam;
    }

    @Override
    public void setSomeParam(String someParam) {
        this.someParam = someParam;
    }

    @Override
    public Integer getMaxNumberOfThreads() {
        if (instance == null)
            return null;
        return instance.getMaxNumberOfThreads();
    }

    // this would be generated:
    private final JmxAttribute threadPoolONJMXAttribute = new JmxAttribute("threadPoolON");

    @Override
    public void validate() {
        checkNotNull(threadPoolON, "Parameter 'threadPool' must be set");
        dependencyResolver.validateDependency(
                TestingThreadPoolServiceInterface.class, threadPoolON,
                threadPoolONJMXAttribute);

        checkState(Strings.isNullOrEmpty(someParam) == false,
                "Parameter 'SomeParam' is blank");
        // check that calling resolveInstance fails
        try {
            dependencyResolver.resolveInstance(TestingThreadPoolIfc.class,
                    threadPoolON, threadPoolONJMXAttribute);
            throw new RuntimeException("fail");
        } catch (IllegalStateException e) {
            checkState("Commit was not triggered".equals(e.getMessage()),
                    e.getMessage());
        }
    }

    @Override
    public Closeable getInstance() {
        if (instance == null) {
            TestingThreadPoolIfc threadPoolInstance = dependencyResolver
                    .resolveInstance(TestingThreadPoolIfc.class, threadPoolON, threadPoolONJMXAttribute);

            if (oldInstance != null) {
                // changing thread pool is not supported
                boolean reuse = threadPoolInstance.equals(oldInstance
                        .getThreadPool());
                if (reuse) {
                    logger.debug("Reusing old instance");
                    instance = oldInstance;
                    instance.setSomeParam(someParam);
                }
            }
            if (instance == null) {
                logger.debug("Creating new instance");
                if (oldCloseable != null) {
                    try {
                        oldCloseable.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                instance = new TestingParallelAPSPImpl(threadPoolInstance,
                        someParam);
            }
        }
        return instance;
    }

    @Override
    public ModuleIdentifier getIdentifier() {
        return identifier;
    }


}
