/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import javax.management.InstanceAlreadyExistsException;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.api.jmx.constants.ConfigRegistryConstants;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.util.jolokia.ConfigTransactionJolokiaClient;

public class ConfigTransactionManagerImplTest extends
        AbstractConfigWithJolokiaTest {

    @Before
    public void setUp() {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver());

    }

    @Test
    public void testSingleton() {
        ConfigRegistryImpl mockedRegistry = mock(ConfigRegistryImpl.class);
        try {
            configRegistryJMXRegistrator.registerToJMX(mockedRegistry);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof InstanceAlreadyExistsException);
        }
    }

    @Test
    public void testCleanUp() {
        super.cleanUpConfigTransactionManagerImpl();
        setUp();
    }

    @Test
    public void testRemoteCallsUsingJMX() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();
        transaction.commit();
    }

    @Test
    public void testRemoteCallsUsingJolokia() throws Exception {

        ConfigTransactionJolokiaClient transactionClient = configRegistryJolokiaClient
                .createTransaction();

        assertEquals("ConfigTransaction-0-1",
                ObjectNameUtil.getTransactionName(transactionClient
                        .getTransactionON()));

        assertEquals(
                ConfigRegistryConstants.ON_DOMAIN
                        + ":TransactionName=ConfigTransaction-0-1,type=ConfigTransaction",
                transactionClient.getTransactionON().getCanonicalName());

        // commit
        transactionClient.commit();

    }
}
