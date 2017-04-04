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
    DOMRpcProviderService domRpcProviderService;
    @Mock
    DOMRpcService domRpcService;
    @Mock
    ActorSystem actorSystem;
    @Mock
    RemoteRpcProviderConfig remoteRpcProviderConfig;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void testCreateInstance() throws Exception {
        Assert.assertNotNull(RemoteRpcProviderFactory
                .createInstance(domRpcProviderService, domRpcService, actorSystem, remoteRpcProviderConfig));
    }
}