/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import akka.dispatch.MessageDispatcher;
import org.junit.Test;
import org.opendaylight.controller.cluster.common.actor.Dispatchers;

public class DispatchersTest {

    @Test
    public void testGetDefaultDispatcherPath(){
        akka.dispatch.Dispatchers mockDispatchers = mock(akka.dispatch.Dispatchers.class);
        doReturn(false).when(mockDispatchers).hasDispatcher(anyString());
        Dispatchers dispatchers = new Dispatchers(mockDispatchers);

        for(Dispatchers.DispatcherType type : Dispatchers.DispatcherType.values()) {
            assertEquals(Dispatchers.DEFAULT_DISPATCHER_PATH,
                    dispatchers.getDispatcherPath(type));
        }

    }

    @Test
    public void testGetDefaultDispatcher(){
        akka.dispatch.Dispatchers mockDispatchers = mock(akka.dispatch.Dispatchers.class);
        MessageDispatcher mockGlobalDispatcher = mock(MessageDispatcher.class);
        doReturn(false).when(mockDispatchers).hasDispatcher(anyString());
        doReturn(mockGlobalDispatcher).when(mockDispatchers).defaultGlobalDispatcher();
        Dispatchers dispatchers = new Dispatchers(mockDispatchers);

        for(Dispatchers.DispatcherType type : Dispatchers.DispatcherType.values()) {
            assertEquals(mockGlobalDispatcher,
                    dispatchers.getDispatcher(type));
        }

    }

    @Test
    public void testGetDispatcherPath(){
        akka.dispatch.Dispatchers mockDispatchers = mock(akka.dispatch.Dispatchers.class);
        doReturn(true).when(mockDispatchers).hasDispatcher(anyString());
        Dispatchers dispatchers = new Dispatchers(mockDispatchers);

        assertEquals(Dispatchers.CLIENT_DISPATCHER_PATH,
                dispatchers.getDispatcherPath(Dispatchers.DispatcherType.Client));

        assertEquals(Dispatchers.TXN_DISPATCHER_PATH,
                dispatchers.getDispatcherPath(Dispatchers.DispatcherType.Transaction));

        assertEquals(Dispatchers.SHARD_DISPATCHER_PATH,
                dispatchers.getDispatcherPath(Dispatchers.DispatcherType.Shard));

        assertEquals(Dispatchers.NOTIFICATION_DISPATCHER_PATH,
                dispatchers.getDispatcherPath(Dispatchers.DispatcherType.Notification));

    }

    @Test
    public void testGetDispatcher(){
        akka.dispatch.Dispatchers mockDispatchers = mock(akka.dispatch.Dispatchers.class);
        MessageDispatcher mockDispatcher = mock(MessageDispatcher.class);
        doReturn(true).when(mockDispatchers).hasDispatcher(anyString());
        doReturn(mockDispatcher).when(mockDispatchers).lookup(anyString());
        Dispatchers dispatchers = new Dispatchers(mockDispatchers);

        assertEquals(Dispatchers.CLIENT_DISPATCHER_PATH,
                dispatchers.getDispatcherPath(Dispatchers.DispatcherType.Client));

        assertEquals(Dispatchers.TXN_DISPATCHER_PATH,
                dispatchers.getDispatcherPath(Dispatchers.DispatcherType.Transaction));

        assertEquals(Dispatchers.SHARD_DISPATCHER_PATH,
                dispatchers.getDispatcherPath(Dispatchers.DispatcherType.Shard));

        assertEquals(Dispatchers.NOTIFICATION_DISPATCHER_PATH,
                dispatchers.getDispatcherPath(Dispatchers.DispatcherType.Notification));

    }
}