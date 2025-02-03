/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.pekko.actor.ActorSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.dom.api.DOMActionProviderService;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMRpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;

@ExtendWith(MockitoExtension.class)
class RemoteOpsProviderFactoryTest {
    @Mock
    private DOMRpcProviderService providerService;
    @Mock
    private DOMRpcService rpcService;
    @Mock
    private ActorSystem actorSystem;
    @Mock
    private RemoteOpsProviderConfig providerConfig;
    @Mock
    private DOMActionProviderService actionProviderService;
    @Mock
    private DOMActionService actionService;

    @Test
    void testCreateInstance() {
        assertNotNull(new RemoteOpsProvider("", actorSystem, providerService, rpcService, providerConfig,
            actionProviderService, actionService));
    }

    @Test
    void testCreateInstanceMissingProvideService() {
        assertThrows(NullPointerException.class, () -> new RemoteOpsProvider("", actorSystem, null, rpcService,
            providerConfig, actionProviderService, actionService));
    }

    @Test
    void testCreateInstanceMissingRpcService() {
        assertThrows(NullPointerException.class, () -> new RemoteOpsProvider("", actorSystem, providerService, null,
            providerConfig, actionProviderService, actionService));
    }

    @Test
    void testCreateInstanceMissingActorSystem() {
        assertThrows(NullPointerException.class, () -> new RemoteOpsProvider("", null, providerService, rpcService,
            providerConfig, actionProviderService, actionService));
    }

    @Test
    void testCreateInstanceMissingProviderConfig() {
        assertThrows(NullPointerException.class, () -> new RemoteOpsProvider("", actorSystem, providerService,
            rpcService, null, actionProviderService, actionService));
    }

    @Test
    void testCreateInstanceMissingActionProvider() {
        assertThrows(NullPointerException.class, () -> new RemoteOpsProvider("", actorSystem, providerService,
            rpcService, providerConfig, null, actionService));
    }

    @Test
    void testCreateInstanceMissingActionService() {
        assertThrows(NullPointerException.class, () -> new RemoteOpsProvider("", actorSystem, providerService,
            rpcService, providerConfig, actionProviderService, null));
    }
}
