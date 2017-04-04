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
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;

public class RemoteRpcProviderFactoryTest {

    @Mock
    private DOMRpcProviderService providerService;
    @Mock
    private DOMRpcService rpcService;
    @Mock
    private ActorSystem actorSystem;
    @Mock
    private RemoteRpcProviderConfig providerConfig;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void testCreateInstance() throws Exception {
        Assert.assertNotNull(RemoteRpcProviderFactory
                .createInstance(providerService, rpcService, actorSystem, providerConfig));
    }

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceMissingProvideService() throws Exception {
        RemoteRpcProviderFactory.createInstance(null, rpcService, actorSystem, providerConfig);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceMissingRpcService() throws Exception {
        RemoteRpcProviderFactory.createInstance(providerService, null, actorSystem, providerConfig);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceMissingActorSystem() throws Exception {
        RemoteRpcProviderFactory.createInstance(providerService, rpcService, null, providerConfig);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceMissingProviderConfig() throws Exception {
        RemoteRpcProviderFactory.createInstance(providerService, rpcService, actorSystem, null);
    }
}