/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

public class TransactionPurgeResponseTest extends AbstractTransactionSuccessTest<TransactionPurgeResponse> {
    private static final TransactionPurgeResponse OBJECT = new TransactionPurgeResponse(
            TRANSACTION_IDENTIFIER, 0);

    @Override
    protected TransactionPurgeResponse object() {
        return OBJECT;
    }

    @Test
    public void cloneAsVersionTest() {
        assertSame(OBJECT, OBJECT.cloneAsVersion(ABIVersion.MAGNESIUM));
    }

    @Override
    protected void doAdditionalAssertions(Object deserialize) {
        assertThat(deserialize, instanceOf(TransactionPurgeResponse.class));
    }
}