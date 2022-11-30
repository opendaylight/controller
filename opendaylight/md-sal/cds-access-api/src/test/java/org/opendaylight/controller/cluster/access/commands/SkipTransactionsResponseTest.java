/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

public class SkipTransactionsResponseTest extends AbstractTransactionSuccessTest<SkipTransactionsResponse> {
    private static final SkipTransactionsResponse OBJECT = new SkipTransactionsResponse(TRANSACTION_IDENTIFIER, 0);

    public SkipTransactionsResponseTest() {
        super(OBJECT, 486);
    }

    @Test
    public void cloneAsVersionTest() {
        final SkipTransactionsResponse clone = OBJECT.cloneAsVersion(ABIVersion.BORON);
        assertEquals(OBJECT, clone);
    }
}