/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

public class TransactionPreCommitRequestTest extends AbstractTransactionRequestTest<TransactionPreCommitRequest> {
    private static final TransactionPreCommitRequest OBJECT = new TransactionPreCommitRequest(TRANSACTION_IDENTIFIER, 0,
        ACTOR_REF);

    public TransactionPreCommitRequestTest() {
        super(OBJECT, 102, 404);
    }

    @Test
    public void cloneAsVersionTest() {
        final var clone = OBJECT.cloneAsVersion(ABIVersion.BORON);
        assertEquals(OBJECT.getSequence(), clone.getSequence());
        assertEquals(OBJECT.getTarget(), clone.getTarget());
        assertEquals(OBJECT.getReplyTo(), clone.getReplyTo());
    }

    @Override
    protected void doAdditionalAssertions(final TransactionPreCommitRequest deserialize) {
        assertEquals(OBJECT.getReplyTo(), deserialize.getReplyTo());
    }
}