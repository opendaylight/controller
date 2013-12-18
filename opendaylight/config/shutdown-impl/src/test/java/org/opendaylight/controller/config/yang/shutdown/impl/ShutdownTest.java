/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.shutdown.impl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.fail;

public class ShutdownTest extends AbstractConfigTest {
    private final ShutdownModuleFactory factory = new ShutdownModuleFactory();
    @Mock
    private BundleContext mockedContext;
    @Mock
    private Bundle mockedSysBundle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(factory));
    }

    @Test
    public void test() {
        fail();
    }
}
