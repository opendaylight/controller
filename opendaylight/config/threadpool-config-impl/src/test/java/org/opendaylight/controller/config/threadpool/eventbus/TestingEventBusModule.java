/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.threadpool.eventbus;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.manager.impl.AbstractMockedModule;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.threadpool.util.CloseableEventBus;
import org.opendaylight.controller.config.yang.threadpool.EventBusServiceInterface;
import org.opendaylight.controller.config.yang.threadpool.impl.EventBusModuleMXBean;

public class TestingEventBusModule extends AbstractMockedModule implements Module, EventBusServiceInterface,
        EventBusModuleMXBean {

    public TestingEventBusModule(DynamicMBeanWithInstance old, ModuleIdentifier id) {
        super(old, id);
    }

    @Override
    protected AutoCloseable prepareMockedInstance() throws Exception {
        CloseableEventBus bus = mock(CloseableEventBus.class);
        doNothing().when(bus).close();
        return bus;
    }

}
