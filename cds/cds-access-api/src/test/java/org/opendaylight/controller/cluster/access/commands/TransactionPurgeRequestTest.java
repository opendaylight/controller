/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

class TransactionPurgeRequestTest extends AbstractTransactionRequestTest<TransactionPurgeRequest> {
    private static final TransactionPurgeRequest OBJECT = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0,
        ACTOR_REF);

    TransactionPurgeRequestTest() {
        super(OBJECT, 101);
    }

    @Test
    void cloneAsVersionTest() {
        final var clone = OBJECT.cloneAsVersion(ABIVersion.TEST_FUTURE_VERSION);
        assertEquals(OBJECT.getSequence(), clone.getSequence());
        assertEquals(OBJECT.getTarget(), clone.getTarget());
        assertEquals(OBJECT.getReplyTo(), clone.getReplyTo());
    }

    @Override
    protected void doAdditionalAssertions(final TransactionPurgeRequest deserialize) {
        assertEquals(OBJECT.getReplyTo(), deserialize.getReplyTo());
    }
}
