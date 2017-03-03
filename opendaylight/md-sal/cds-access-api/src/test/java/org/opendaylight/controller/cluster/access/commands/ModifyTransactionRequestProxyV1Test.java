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
import com.google.common.collect.Lists;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestProxyTest;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

public class ModifyTransactionRequestProxyV1Test extends AbstractRequestProxyTest<ModifyTransactionRequestProxyV1> {

    private static final ClientIdentifier CLIENT_IDENTIFIER = ClientIdentifier.create(
            FrontendIdentifier.create(MemberName.forName("test"), FrontendType.forName("one")), 0
    );
    private static final LocalHistoryIdentifier HISTORY_IDENTIFIER = new LocalHistoryIdentifier(CLIENT_IDENTIFIER, 0);
    private static final TransactionIdentifier TRANSACTION_IDENTIFIER = new TransactionIdentifier(HISTORY_IDENTIFIER, 0);

    private static final ActorSystem SYSTEM = ActorSystem.create("test");
    private static final ActorRef ACTOR_REF = TestProbe.apply(SYSTEM).ref();

    private static final PersistenceProtocol PROTOCOL = PersistenceProtocol.ABORT;

    private static final ModifyTransactionRequest REQUEST = new ModifyTransactionRequest(TRANSACTION_IDENTIFIER, 0, ACTOR_REF,
            Lists.newArrayList(), PROTOCOL);
    private static final ModifyTransactionRequestProxyV1 OBJECT = new ModifyTransactionRequestProxyV1(REQUEST);

    @Override
    public ModifyTransactionRequestProxyV1 object() {
        return OBJECT;
    }

    @Test
    public void readExternal() throws Exception {

    }

    @Test
    public void writeExternal() throws Exception {

    }

    @Test
    public void createRequest() throws Exception {

    }
}