/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.threadpool.fixed;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.manager.impl.AbstractMockedModule;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.threadpool.util.FixedThreadPoolWrapper;
import org.opendaylight.controller.config.yang.threadpool.ThreadPoolServiceInterface;

public class TestingFixedThreadPoolModule extends AbstractMockedModule implements ThreadPoolServiceInterface, Module {

    public TestingFixedThreadPoolModule(DynamicMBeanWithInstance old, ModuleIdentifier id) {
        super(old, id);
    }

    @Override
    protected AutoCloseable prepareMockedInstance() throws Exception {
        FixedThreadPoolWrapper pool = mock(FixedThreadPoolWrapper.class);
        doNothing().when(pool).close();
        doReturn(mock(ExecutorService.class)).when(pool).getExecutor();
        return pool;
    }
}
