/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import org.junit.After;
import org.junit.Before;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.ModuleFactoriesResolver;
import org.opendaylight.controller.config.util.JolokiaHelper;
import org.opendaylight.controller.config.util.jolokia.ConfigRegistryJolokiaClient;

public class AbstractConfigWithJolokiaTest extends AbstractConfigTest {
    protected String jolokiaURL;
    protected ConfigRegistryJolokiaClient configRegistryJolokiaClient;

    @Before
    public void initJolokia() {
        jolokiaURL = JolokiaHelper.startTestingJolokia();
    }

    // this method should be called in @Before
    @Override
    protected void initConfigTransactionManagerImpl(
            ModuleFactoriesResolver resolver) {
        super.initConfigTransactionManagerImpl(resolver);
        configRegistryJolokiaClient = new ConfigRegistryJolokiaClient(
                jolokiaURL);
    }

    @After
    public void cleanUpJolokia() {
        JolokiaHelper.stopJolokia();
    }
}
