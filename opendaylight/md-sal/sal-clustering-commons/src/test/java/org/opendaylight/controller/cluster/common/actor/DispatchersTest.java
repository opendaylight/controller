/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.apache.pekko.dispatch.MessageDispatcher;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.common.actor.Dispatchers.DispatcherType;

class DispatchersTest {
    @Test
    void testGetDefaultDispatcherPath() {
        final var mockDispatchers = mock(org.apache.pekko.dispatch.Dispatchers.class);
        doReturn(false).when(mockDispatchers).hasDispatcher(anyString());
        final var dispatchers = new Dispatchers(mockDispatchers);

        for (var type : DispatcherType.values()) {
            assertEquals(DispatcherType.DEFAULT_DISPATCHER_PATH, dispatchers.getDispatcherPath(type));
        }
    }

    @Test
    void testGetDefaultDispatcher() {
        final var mockDispatchers = mock(org.apache.pekko.dispatch.Dispatchers.class);
        final var mockGlobalDispatcher = mock(MessageDispatcher.class);
        doReturn(false).when(mockDispatchers).hasDispatcher(anyString());
        doReturn(mockGlobalDispatcher).when(mockDispatchers).defaultGlobalDispatcher();
        final var dispatchers = new Dispatchers(mockDispatchers);

        for (var type : DispatcherType.values()) {
            assertEquals(mockGlobalDispatcher, dispatchers.getDispatcher(type));
        }
    }

    @Test
    void testGetDispatcherPath() {
        final var mockDispatchers = mock(org.apache.pekko.dispatch.Dispatchers.class);
        doReturn(true).when(mockDispatchers).hasDispatcher(anyString());
        final var dispatchers = new Dispatchers(mockDispatchers);

        assertEquals("client-dispatcher", dispatchers.getDispatcherPath(DispatcherType.Client));
        assertEquals("txn-dispatcher", dispatchers.getDispatcherPath(DispatcherType.Transaction));
        assertEquals("shard-dispatcher", dispatchers.getDispatcherPath(DispatcherType.Shard));
        assertEquals("notification-dispatcher", dispatchers.getDispatcherPath(DispatcherType.Notification));
    }

    @Test
    void testGetDispatcher() {
        final var mockDispatchers = mock(org.apache.pekko.dispatch.Dispatchers.class);
        final var mockDispatcher = mock(MessageDispatcher.class);
        doReturn(true).when(mockDispatchers).hasDispatcher(anyString());
        doReturn(mockDispatcher).when(mockDispatchers).lookup(anyString());
        final var dispatchers = new Dispatchers(mockDispatchers);

        assertSame(mockDispatcher, dispatchers.getDispatcher(DispatcherType.Client));
        assertSame(mockDispatcher, dispatchers.getDispatcher(DispatcherType.Transaction));
        assertSame(mockDispatcher, dispatchers.getDispatcher(DispatcherType.Shard));
        assertSame(mockDispatcher, dispatchers.getDispatcher(DispatcherType.Notification));
    }
}
