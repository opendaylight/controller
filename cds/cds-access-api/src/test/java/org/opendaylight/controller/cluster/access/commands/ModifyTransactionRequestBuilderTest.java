/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.testkit.TestActors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;

class ModifyTransactionRequestBuilderTest {
    private final MemberName memberName = MemberName.forName("member-1");
    private final FrontendType frontendType = FrontendType.forName("test");
    private final FrontendIdentifier frontendId = FrontendIdentifier.create(memberName, frontendType);
    private final ClientIdentifier clientId = ClientIdentifier.create(frontendId, 0);
    private final TransactionIdentifier transactionIdentifier =
            new TransactionIdentifier(new LocalHistoryIdentifier(clientId, 0L), 0L);
    private final ActorRef actorRef = ActorSystem.create("test").actorOf(Props.create(TestActors.EchoActor.class));
    private final NormalizedNode node = ImmutableNodes.newContainerBuilder()
        .withNodeIdentifier(new NodeIdentifier(QName.create("namespace", "localName")))
        .build();
    private final TransactionModification transactionModification =
            new TransactionWrite(YangInstanceIdentifier.of(), node);
    private final ModifyTransactionRequestBuilder modifyTransactionRequestBuilder =
            ModifyTransactionRequest.builder(transactionIdentifier, actorRef);

    @BeforeEach
    void beforeEach() {
        modifyTransactionRequestBuilder.setSequence(0L);
        modifyTransactionRequestBuilder.addModification(transactionModification);
        assertEquals(1, modifyTransactionRequestBuilder.size());
    }

    @Test
    void testGetIdentifier() {
        assertEquals(transactionIdentifier, modifyTransactionRequestBuilder.getIdentifier());
    }

    @Test
    void testBuildReady() {
        modifyTransactionRequestBuilder.setReady();
        final var modifyTransactionRequest = modifyTransactionRequestBuilder.build();
        assertEquals(PersistenceProtocol.READY, modifyTransactionRequest.getPersistenceProtocol().orElseThrow());
        assertEquals(transactionModification, modifyTransactionRequest.getModifications().get(0));
    }

    @Test
    void testBuildAbort() {
        modifyTransactionRequestBuilder.setAbort();
        final var modifyTransactionRequest = modifyTransactionRequestBuilder.build();
        assertEquals(PersistenceProtocol.ABORT, modifyTransactionRequest.getPersistenceProtocol().orElseThrow());
        assertTrue(modifyTransactionRequest.getModifications().isEmpty());
    }

    @Test
    void testBuildCommitTrue() {
        modifyTransactionRequestBuilder.setCommit(true);
        final var modifyTransactionRequest = modifyTransactionRequestBuilder.build();
        assertEquals(PersistenceProtocol.THREE_PHASE, modifyTransactionRequest.getPersistenceProtocol().orElseThrow());
    }

    @Test
    void testBuildCommitFalse() {
        modifyTransactionRequestBuilder.setCommit(false);
        final var modifyTransactionRequest = modifyTransactionRequestBuilder.build();
        assertEquals(PersistenceProtocol.SIMPLE, modifyTransactionRequest.getPersistenceProtocol().orElseThrow());
    }
}
