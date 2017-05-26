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

public class TransactionCommitSuccessTest extends AbstractTransactionSuccessTest<TransactionCommitSuccess> {
    private static final TransactionCommitSuccess OBJECT = new TransactionCommitSuccess(
            TRANSACTION_IDENTIFIER, 0);

    @Override
    protected TransactionCommitSuccess object() {
        return OBJECT;
    }

    @Test
    public void cloneAsVersionTest() throws Exception {
        final TransactionCommitSuccess clone = OBJECT.cloneAsVersion(ABIVersion.BORON);
        Assert.assertEquals(OBJECT, clone);
    }

    @Override
    protected void doAdditionalAssertions(Object deserialize) {
        Assert.assertTrue(deserialize instanceof TransactionCommitSuccess);
    }
}