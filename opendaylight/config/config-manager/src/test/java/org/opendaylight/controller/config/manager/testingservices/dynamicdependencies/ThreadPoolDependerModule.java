/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.dynamicdependencies;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.manager.testingservices.seviceinterface.TestingThreadPoolServiceInterface;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingThreadPoolIfc;
import org.opendaylight.controller.config.spi.Module;

import java.util.Map;
import java.util.Set;

public class ThreadPoolDependerModule implements Module, ThreadPoolDependerModuleMXBean {
    private final ModuleIdentifier mi;
    private final DependencyResolver dependencyResolver;

    public ThreadPoolDependerModule(ModuleIdentifier mi, DependencyResolver dependencyResolver) {
        this.mi = mi;
        this.dependencyResolver = dependencyResolver;
    }

    @Override
    public void validate() {
        try {
            dependencyResolver.validateDependencies(WrongServiceInterface.class);
            throw new IllegalStateException("should fail");
        } catch (IllegalArgumentException e) {
            // good
        }
        Set<ModuleIdentifier> dynamicTPs = dependencyResolver.validateDependencies(TestingThreadPoolServiceInterface.class);
        Preconditions.checkState(dynamicTPs.size() > 0);
    }

    private AutoCloseable instance;

    @Override
    public AutoCloseable getInstance() {
        if (instance == null) {
            instance = createInstance();
        }
        return instance;
    }

    private AutoCloseable createInstance() {
        Map<ModuleIdentifier, TestingThreadPoolIfc> threadPools = dependencyResolver.resolveInstances(TestingThreadPoolServiceInterface.class, TestingThreadPoolIfc.class);
        return new DependerInstance(threadPools);
    }

    @Override
    public ModuleIdentifier getIdentifier() {
        return mi;
    }

    public static interface WrongServiceInterface extends AbstractServiceInterface {

    }

    public static class DependerInstance implements AutoCloseable {
        Map<ModuleIdentifier, TestingThreadPoolIfc> threadPools;

        DependerInstance(Map<ModuleIdentifier, TestingThreadPoolIfc> threadPools) {
            this.threadPools = threadPools;
        }

        public Map<ModuleIdentifier, TestingThreadPoolIfc> getThreadPools() {
            return threadPools;
        }

        @Override
        public void close() {

        }
    }

}

interface ThreadPoolDependerModuleMXBean {

}
