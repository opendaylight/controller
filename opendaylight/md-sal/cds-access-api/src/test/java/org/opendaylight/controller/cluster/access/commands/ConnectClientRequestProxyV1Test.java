/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.TestProbe;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestProxyTest;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;

public class ConnectClientRequestProxyV1Test extends AbstractRequestProxyTest<ConnectClientRequestProxyV1> {

    private static final FrontendIdentifier FRONTEND_IDENTIFIER = FrontendIdentifier.create(
            MemberName.forName("test"), FrontendType.forName("one"));
    private static final ClientIdentifier IDENTIFIER = ClientIdentifier.create(FRONTEND_IDENTIFIER, 0);
    private static final ActorSystem SYSTEM = ActorSystem.create("test");
    private static final ActorRef ACTOR_REF = TestProbe.apply(SYSTEM).ref();
    private static final ConnectClientRequest REQUEST = new ConnectClientRequest(
            IDENTIFIER, ACTOR_REF, ABIVersion.TEST_PAST_VERSION, ABIVersion.TEST_FUTURE_VERSION);

    private static final ConnectClientRequestProxyV1 OBJECT = new ConnectClientRequestProxyV1(REQUEST);

    @Override
    public ConnectClientRequestProxyV1 object() {
        return OBJECT;
    }

    @Test
    public void createRequest() throws Exception {
        OBJECT.createRequest(IDENTIFIER, 0, ACTOR_REF);
    }

    @Test
    public void readTarget() throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(new byte[]{1}));
        OBJECT.readTarget(in);
    }
}