/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.primitives.UnsignedLong;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

class SkipTransactionsRequestTest extends AbstractTransactionRequestTest<SkipTransactionsRequest> {
    private static final SkipTransactionsRequest OBJECT = new SkipTransactionsRequest(TRANSACTION_IDENTIFIER, 0,
        ACTOR_REF, List.of(UnsignedLong.ONE));

    SkipTransactionsRequestTest() {
        super(OBJECT, 109);
    }

    @Test
    void cloneAsVersionTest() {
        final var clone = OBJECT.cloneAsVersion(ABIVersion.TEST_FUTURE_VERSION);
        assertEquals(OBJECT.getSequence(), clone.getSequence());
        assertEquals(OBJECT.getTarget(), clone.getTarget());
        assertEquals(OBJECT.getReplyTo(), clone.getReplyTo());
    }

    @Override
    protected void doAdditionalAssertions(final SkipTransactionsRequest deserialize) {
        assertEquals(OBJECT.getReplyTo(), deserialize.getReplyTo());
    }
}
