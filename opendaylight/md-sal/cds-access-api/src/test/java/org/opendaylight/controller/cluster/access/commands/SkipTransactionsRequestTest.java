/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.google.common.primitives.UnsignedLong;
import java.util.List;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

public class SkipTransactionsRequestTest extends AbstractTransactionRequestTest<SkipTransactionsRequest> {
    private static final SkipTransactionsRequest OBJECT = new SkipTransactionsRequest(
            TRANSACTION_IDENTIFIER, 0, ACTOR_REF, List.of(UnsignedLong.ONE));

    @Override
    protected SkipTransactionsRequest object() {
        return OBJECT;
    }

    @Test
    public void cloneAsVersionTest() {
        assertSame(OBJECT, OBJECT.cloneAsVersion(ABIVersion.MAGNESIUM));
    }

    @Override
    protected void doAdditionalAssertions(final Object deserialize) {
        assertThat(deserialize, instanceOf(SkipTransactionsRequest.class));
        assertEquals(OBJECT.getReplyTo(), ((SkipTransactionsRequest) deserialize).getReplyTo());
    }
}