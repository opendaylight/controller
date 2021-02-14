/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.TestActors;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;

public class ModifyTransactionRequestBuilderTest {

    private final MemberName memberName = MemberName.forName("member-1");
    private final FrontendType frontendType = FrontendType.forName("test");
    private final FrontendIdentifier frontendId = FrontendIdentifier.create(memberName, frontendType);
    private final ClientIdentifier clientId = ClientIdentifier.create(frontendId, 0);
    private final TransactionIdentifier transactionIdentifier =
            new TransactionIdentifier(new LocalHistoryIdentifier(clientId, 0L), 0L);
    private final ActorRef actorRef = ActorSystem.create("test").actorOf(Props.create(TestActors.EchoActor.class));
    private final NormalizedNode node = Builders.containerBuilder().withNodeIdentifier(
            YangInstanceIdentifier.NodeIdentifier.create(QName.create("namespace", "localName"))).build();
    private final TransactionModification transactionModification =
            new TransactionWrite(YangInstanceIdentifier.empty(), node);
    private final ModifyTransactionRequestBuilder modifyTransactionRequestBuilder =
            new ModifyTransactionRequestBuilder(transactionIdentifier, actorRef);

    @Before
    public void setUp() {
        modifyTransactionRequestBuilder.setSequence(0L);
        modifyTransactionRequestBuilder.addModification(transactionModification);
        assertEquals(1, modifyTransactionRequestBuilder.size());
    }

    @Test
    public void testGetIdentifier() {
        final TransactionIdentifier identifier = modifyTransactionRequestBuilder.getIdentifier();
        assertEquals(transactionIdentifier, identifier);
    }

    @Test
    public void testBuildReady() {
        modifyTransactionRequestBuilder.setReady();
        final ModifyTransactionRequest modifyTransactionRequest = modifyTransactionRequestBuilder.build();
        assertEquals(PersistenceProtocol.READY, modifyTransactionRequest.getPersistenceProtocol().get());
        assertEquals(transactionModification, modifyTransactionRequest.getModifications().get(0));
    }

    @Test
    public void testBuildAbort() {
        modifyTransactionRequestBuilder.setAbort();
        final ModifyTransactionRequest modifyTransactionRequest = modifyTransactionRequestBuilder.build();
        assertEquals(PersistenceProtocol.ABORT, modifyTransactionRequest.getPersistenceProtocol().get());
        assertTrue(modifyTransactionRequest.getModifications().isEmpty());
    }

    @Test
    public void testBuildCommitTrue() {
        modifyTransactionRequestBuilder.setCommit(true);
        final ModifyTransactionRequest modifyTransactionRequest = modifyTransactionRequestBuilder.build();
        assertEquals(PersistenceProtocol.THREE_PHASE, modifyTransactionRequest.getPersistenceProtocol().get());
    }

    @Test
    public void testBuildCommitFalse() {
        modifyTransactionRequestBuilder.setCommit(false);
        final ModifyTransactionRequest modifyTransactionRequest = modifyTransactionRequestBuilder.build();
        assertEquals(PersistenceProtocol.SIMPLE, modifyTransactionRequest.getPersistenceProtocol().get());
    }

}
