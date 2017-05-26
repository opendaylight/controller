/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

public class TransactionDoCommitRequestTest extends AbstractTransactionRequestTest<TransactionDoCommitRequest> {
    private static final TransactionDoCommitRequest OBJECT = new TransactionDoCommitRequest(
            TRANSACTION_IDENTIFIER, 0, ACTOR_REF);

    @Override
    protected TransactionDoCommitRequest object() {
        return OBJECT;
    }

    @Test
    public void cloneAsVersionTest() throws Exception {
        final TransactionDoCommitRequest clone = OBJECT.cloneAsVersion(ABIVersion.BORON);
        Assert.assertEquals(OBJECT, clone);
    }

    @Override
    protected void doAdditionalAssertions(final Object deserialize) {
        Assert.assertTrue(deserialize instanceof TransactionDoCommitRequest);
        Assert.assertEquals(OBJECT.getReplyTo(), ((TransactionDoCommitRequest) deserialize).getReplyTo());
    }
}