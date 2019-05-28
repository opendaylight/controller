/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static org.mockito.MockitoAnnotations.initMocks;

import akka.actor.ActorSystem;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.dom.api.DOMActionProviderService;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMRpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;

public class RemoteRpcProviderFactoryTest {

    @Mock
    private DOMRpcProviderService providerService;
    @Mock
    private DOMRpcService rpcService;
    @Mock
    private ActorSystem actorSystem;
    @Mock
    private RemoteRpcProviderConfig providerConfig;
    @Mock
    private DOMActionProviderService actionProviderService;
    @Mock
    private DOMActionService actionService;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testCreateInstance() {
        Assert.assertNotNull(RemoteRpcProviderFactory
                .createInstance(providerService, rpcService, actorSystem, providerConfig, actionProviderService, actionService));
    }

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceMissingProvideService() {
        RemoteRpcProviderFactory.createInstance(null, rpcService, actorSystem, providerConfig, actionProviderService, actionService);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceMissingRpcService() {
        RemoteRpcProviderFactory.createInstance(providerService, null, actorSystem, providerConfig, actionProviderService, actionService);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceMissingActorSystem() {
        RemoteRpcProviderFactory.createInstance(providerService, rpcService, null, providerConfig, actionProviderService, actionService);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceMissingProviderConfig() {
        RemoteRpcProviderFactory.createInstance(providerService, rpcService, actorSystem, null, actionProviderService, actionService);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceMissingActionProvider() {
        RemoteRpcProviderFactory.createInstance(providerService, rpcService, actorSystem, providerConfig, null, actionService);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceMissingActionService() {
        RemoteRpcProviderFactory.createInstance(providerService, rpcService, actorSystem, providerConfig, actionProviderService, null);
    }
}
