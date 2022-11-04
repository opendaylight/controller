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
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

public class SkipTransactionsResponseTest extends AbstractTransactionSuccessTest<SkipTransactionsResponse> {
    private static final SkipTransactionsResponse OBJECT = new SkipTransactionsResponse(TRANSACTION_IDENTIFIER, 0);

    @Override
    protected SkipTransactionsResponse object() {
        return OBJECT;
    }

    @Test
    public void cloneAsVersionTest() {
        assertSame(OBJECT, OBJECT.cloneAsVersion(ABIVersion.MAGNESIUM));
    }

    @Override
    protected void doAdditionalAssertions(final Object deserialize) {
        assertThat(deserialize, instanceOf(SkipTransactionsResponse.class));
    }
}