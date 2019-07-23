/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.ConfigFactory;
import java.util.Collections;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class RpcListenerTest {

    private static final QName TEST_QNAME = QName.create("test", "2015-06-12", "test");
    private static final SchemaPath RPC_TYPE = SchemaPath.create(true, TEST_QNAME);
    private static final YangInstanceIdentifier TEST_PATH = YangInstanceIdentifier
            .create(new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME));
    private static final DOMRpcIdentifier RPC_ID = DOMRpcIdentifier.create(RPC_TYPE, TEST_PATH);

    private static ActorSystem SYSTEM;

    @BeforeClass
    public static void setup() {
        SYSTEM = ActorSystem.create("opendaylight-rpc", ConfigFactory.load().getConfig("odl-cluster-rpc"));
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(SYSTEM);
        SYSTEM = null;
    }

    @Test
    public void testRouteAdd() {
        // Test announcements
        final TestKit probeReg = new TestKit(SYSTEM);
        final ActorRef rpcRegistry = probeReg.getRef();

        final RpcListener rpcListener = new RpcListener(rpcRegistry);
        rpcListener.onRpcAvailable(Collections.singleton(RPC_ID));
        probeReg.expectMsgClass(RpcRegistry.Messages.AddOrUpdateRoutes.class);
    }

    @Test
    public void testRouteRemove() {
        // Test announcements
        final TestKit probeReg = new TestKit(SYSTEM);
        final ActorRef rpcRegistry = probeReg.getRef();

        final RpcListener rpcListener = new RpcListener(rpcRegistry);
        rpcListener.onRpcUnavailable(Collections.singleton(RPC_ID));
        probeReg.expectMsgClass(RpcRegistry.Messages.RemoveRoutes.class);
    }
}
