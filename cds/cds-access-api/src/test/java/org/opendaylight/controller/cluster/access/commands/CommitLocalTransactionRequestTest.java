/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.base.MoreObjects;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;

class CommitLocalTransactionRequestTest extends AbstractLocalTransactionRequestTest<CommitLocalTransactionRequest> {
    private static final FrontendIdentifier FRONTEND = FrontendIdentifier.create(
            MemberName.forName("test"), FrontendType.forName("one"));
    private static final ClientIdentifier CLIENT = ClientIdentifier.create(FRONTEND, 0);
    private static final LocalHistoryIdentifier HISTORY = new LocalHistoryIdentifier(CLIENT, 0);
    private static final TransactionIdentifier TRANSACTION = new TransactionIdentifier(HISTORY, 0);

    private static final DataTreeModification MODIFICATION = Mockito.mock(DataTreeModification.class);
    private static final boolean COORDINATED = true;

    private static final CommitLocalTransactionRequest OBJECT = new CommitLocalTransactionRequest(TRANSACTION, 0,
        ACTOR_REF, MODIFICATION, null, COORDINATED);

    CommitLocalTransactionRequestTest() {
        super(OBJECT);
    }

    @Test
    void getModificationTest() {
        assertEquals(MODIFICATION, OBJECT.getModification());
    }

    @Test
    void isCoordinatedTest() {
        assertEquals(COORDINATED, OBJECT.isCoordinated());
    }

    @Test
    void addToStringAttributesTest() {
        final var result = OBJECT.addToStringAttributes(MoreObjects.toStringHelper(OBJECT)).toString();
        assertThat(result).contains("coordinated=" + COORDINATED);
    }

    @Override
    protected void doAdditionalAssertions(final CommitLocalTransactionRequest deserialize) {
        assertEquals(OBJECT.getReplyTo(), deserialize.getReplyTo());
        assertEquals(OBJECT.getModification(), deserialize.getModification());
    }
}
