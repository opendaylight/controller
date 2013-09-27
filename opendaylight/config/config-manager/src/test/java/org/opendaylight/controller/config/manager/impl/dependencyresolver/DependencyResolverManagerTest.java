/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.dependencyresolver;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.JmxAttribute;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.ModuleInternalTransactionalInfo;
import org.opendaylight.controller.config.manager.impl.TransactionStatus;
import org.opendaylight.controller.config.spi.Module;

public class DependencyResolverManagerTest {

    final ModuleIdentifier apspName = new ModuleIdentifier("apsp", "apsp"); // depends
                                                                            // on:
    final ModuleIdentifier threadPoolName = new ModuleIdentifier("threadpool",
            "threadpool"); // depends on:
    final ModuleIdentifier threadFactoryName = new ModuleIdentifier(
            "threadfactory", "threadfactory");

    private DependencyResolverManager tested;
    TransactionStatus transactionStatus;

    @Before
    public void setUp() {
        transactionStatus = mock(TransactionStatus.class);
        tested = new DependencyResolverManager("txName", transactionStatus);
        doNothing().when(transactionStatus).checkCommitStarted();
        doNothing().when(transactionStatus).checkNotCommitted();
    }

    @Test
    public void testOrdering() {
        DependencyResolverImpl apspDRI = tested.getOrCreate(apspName);
        mockGetInstance(tested, apspName);
        DependencyResolverImpl threadPoolDRI = tested
                .getOrCreate(threadPoolName);
        mockGetInstance(tested, threadPoolName);
        tested.getOrCreate(threadFactoryName);
        mockGetInstance(tested, threadFactoryName);

        // set threadfactory as dependency of threadpool
        declareDependency(threadPoolDRI, threadFactoryName);
        // set threadpool as dependency of apsp
        declareDependency(apspDRI, threadPoolName);

        // switch to second phase committed
        reset(transactionStatus);
        doNothing().when(transactionStatus).checkCommitted();
        List<ModuleIdentifier> sortedModuleIdentifiers = tested
                .getSortedModuleIdentifiers();
        assertEquals(
                Arrays.asList(threadFactoryName, threadPoolName, apspName),
                sortedModuleIdentifiers);

    }

    /**
     * Simulate dependentResolver resolving its dependency identified by
     * dependentName.
     */
    private void declareDependency(DependencyResolverImpl dependerResolver,
            ModuleIdentifier dependentName) {
        JmxAttribute dummyAttribute = new JmxAttribute("dummy");
        dependerResolver.resolveInstance(Object.class,
                ObjectNameUtil.createReadOnlyModuleON(dependentName),
                dummyAttribute);
    }

    private static void mockGetInstance(DependencyResolverManager tested,
            ModuleIdentifier moduleIdentifier) {
        ModuleInternalTransactionalInfo mock = mock(ModuleInternalTransactionalInfo.class);
        doReturn(moduleIdentifier).when(mock).getName();
        doReturn(mockedModule()).when(mock).getModule();
        tested.put(mock);
    }

    private static Module mockedModule() {
        Module mockedModule = mock(Module.class);
        doReturn(mock(AutoCloseable.class)).when(mockedModule).getInstance();
        return mockedModule;
    }

}
