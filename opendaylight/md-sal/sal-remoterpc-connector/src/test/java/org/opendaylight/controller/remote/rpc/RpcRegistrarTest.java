/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMRpcRouter;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

public class RpcRegistrarTest {

    private RpcRegistrar rpcRegistrar;
    private DOMRpcProviderService providerService;

    @Mock
    private RemoteRpcProviderConfig providerConfig;
    @Mock
    private ListenerRegistration<SchemaContextListener> listenerRegistration;
    @Mock
    private SchemaService schemaService;

    @Before
    public void setUp() throws Exception {
        providerService = new DOMRpcRouter();

        //akka.actor.ActorInitializationException:
        // You cannot create an instance of
        // [org.opendaylight.controller.remote.rpc.RpcRegistrar] explicitly using the constructor (new).
        // You have to use one of the 'actorOf' factory methods to create a new actor. See the documentation.

        //rpcRegistrar = new RpcRegistrar(providerConfig, providerService);
    }

    @Test
    public void testProps() throws Exception {
        final Props props = RpcRegistrar.props(providerConfig, providerService);
        final ActorSystem actorSystem = ActorSystem.apply("bar");
        final ActorRef actorRef = actorSystem.actorOf(props, "testActor");
    }

    @Test
    public void testPostStop() throws Exception {
        //rpcRegistrar.postStop();
    }

    @Test
    public void testHandleReceive() throws Exception {
        //rpcRegistrar.handleReceive();
    }
}