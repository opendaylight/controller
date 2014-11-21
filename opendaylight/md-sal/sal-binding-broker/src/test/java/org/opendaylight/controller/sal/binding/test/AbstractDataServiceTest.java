/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Before;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.test.util.BindingBrokerTestFactory;
import org.opendaylight.controller.sal.binding.test.util.BindingTestContext;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;

public abstract class AbstractDataServiceTest {

    protected org.opendaylight.controller.sal.core.api.data.DataProviderService biDataService;
    protected DataProviderService baDataService;
    protected BindingIndependentMappingService mappingService;
    protected BindingTestContext testContext;

    @Before
    public void setUp() {
        ListeningExecutorService executor = MoreExecutors.sameThreadExecutor();
        BindingBrokerTestFactory factory = new BindingBrokerTestFactory();
        factory.setExecutor(executor);
        factory.setStartWithParsedSchema(getStartWithSchema());
        testContext = factory.getTestContext();
        testContext.start();

        baDataService = testContext.getBindingDataBroker();
        biDataService = testContext.getDomDataBroker();
        mappingService = testContext.getBindingToDomMappingService();
    }

    protected boolean getStartWithSchema() {
        return true;
    }
}
