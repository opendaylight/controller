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
import org.opendaylight.controller.sal.binding.test.util.BindingBrokerTestFactory;
import org.opendaylight.controller.sal.binding.test.util.BindingTestContext;

public abstract class AbstractDataServiceTest {

    protected BindingTestContext testContext;

    @Before
    public void setUp() {
        ListeningExecutorService executor = MoreExecutors.newDirectExecutorService();
        BindingBrokerTestFactory factory = new BindingBrokerTestFactory();
        factory.setExecutor(executor);
        factory.setStartWithParsedSchema(getStartWithSchema());
        testContext = factory.getTestContext();
        testContext.start();
    }

    protected boolean getStartWithSchema() {
        return true;
    }
}
