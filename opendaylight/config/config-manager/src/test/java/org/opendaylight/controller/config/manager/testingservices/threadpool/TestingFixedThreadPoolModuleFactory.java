/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.threadpool;

import java.util.Arrays;
import java.util.List;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.manager.testingservices.seviceinterface.ModifiableThreadPoolServiceInterface;
import org.opendaylight.controller.config.manager.testingservices.seviceinterface.TestingThreadPoolServiceInterface;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;

public class TestingFixedThreadPoolModuleFactory implements ModuleFactory {
    public static final String NAME = "fixed";
    private static List<Class<? extends TestingThreadPoolServiceInterface>> ifc = Arrays
            .asList(ModifiableThreadPoolServiceInterface.class, TestingThreadPoolServiceInterface.class);

    @Override
    public String getImplementationName() {
        return NAME;
    }

    @Override
    public TestingFixedThreadPoolModule createModule(String instanceName,
            DependencyResolver dependencyResolver) {
        return new TestingFixedThreadPoolModule(new ModuleIdentifier(NAME,
                instanceName), null, null);
    }

    @Override
    public Module createModule(String instanceName,
            DependencyResolver dependencyResolver, DynamicMBeanWithInstance old)
            throws Exception {
        int threadCount = (Integer) old.getAttribute("ThreadCount");
        // is the instance compatible?
        TestingFixedThreadPool oldInstance;
        try {
            // reconfigure existing instance
            oldInstance = (TestingFixedThreadPool) old.getInstance();
        } catch (ClassCastException e) {
            // old instance will be closed, new needs to be created
            oldInstance = null;
        }
        TestingFixedThreadPoolModule result = new TestingFixedThreadPoolModule(
                new ModuleIdentifier(NAME, instanceName), old.getInstance(),
                oldInstance);
        result.setThreadCount(threadCount);
        return result;
    }

    @Override
    public boolean isModuleImplementingServiceInterface(
            Class<? extends AbstractServiceInterface> serviceInterface) {
        return ifc.contains(serviceInterface);
    }
}
