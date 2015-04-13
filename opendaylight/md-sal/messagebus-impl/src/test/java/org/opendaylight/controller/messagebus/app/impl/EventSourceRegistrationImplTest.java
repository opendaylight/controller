/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.app.impl;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.messagebus.spi.EventSource;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class EventSourceRegistrationImplTest {

    EventSourceRegistrationImplLocal eventSourceRegistrationImplLocal;
    EventSourceTopology eventSourceTopologyMock;

    @BeforeClass
    public static void initTestClass() throws IllegalAccessException, InstantiationException {
    }

    @Before
    public void setUp() throws Exception {
        EventSource eventSourceMock = mock(EventSource.class);
        eventSourceTopologyMock = mock(EventSourceTopology.class);
        eventSourceRegistrationImplLocal = new EventSourceRegistrationImplLocal(eventSourceMock, eventSourceTopologyMock);
    }

    @Test
    public void removeRegistrationTest() {
        eventSourceRegistrationImplLocal.removeRegistration();
        verify(eventSourceTopologyMock, times(1)).unRegister(any(EventSource.class));
    }


    private class EventSourceRegistrationImplLocal extends EventSourceRegistrationImpl{

        /**
         * @param instance            of EventSource that has been registered by {@link EventSourceRegistryImpl#registerEventSource(Node, org.opendaylight.controller.messagebus.spi.EventSource)}
         * @param eventSourceTopology
         */
        public EventSourceRegistrationImplLocal(EventSource instance, EventSourceTopology eventSourceTopology) {
            super(instance, eventSourceTopology);
        }
    }

}