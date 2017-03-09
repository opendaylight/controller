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

public class TransactionPreCommitSuccessProxyV1Test
        extends AbstractTransactionSuccessProxyTest<TransactionPreCommitSuccessProxyV1> {
    private static final TransactionPreCommitSuccess REQUEST = new TransactionPreCommitSuccess(
            TRANSACTION_IDENTIFIER, 0);
    private static final TransactionPreCommitSuccessProxyV1 OBJECT = new TransactionPreCommitSuccessProxyV1(REQUEST);

    @Override
    TransactionPreCommitSuccessProxyV1 object() {
        return OBJECT;
    }

    @Test
    public void createSuccess() throws Exception {
        final TransactionPreCommitSuccess result = OBJECT.createSuccess(TRANSACTION_IDENTIFIER, 0);
        Assert.assertNotNull(result);
    }
}