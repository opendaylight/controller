/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

public class TransactionCanCommitSuccessTest extends AbstractTransactionSuccessTest<TransactionCanCommitSuccess> {
    private static final TransactionCanCommitSuccess OBJECT = new TransactionCanCommitSuccess(TRANSACTION_IDENTIFIER,
        0);

    public TransactionCanCommitSuccessTest() {
        super(OBJECT, 489);
    }

    @Test
    public void cloneAsVersionTest() {
        final TransactionCanCommitSuccess clone = OBJECT.cloneAsVersion(ABIVersion.BORON);
        assertEquals(OBJECT, clone);
    }
}