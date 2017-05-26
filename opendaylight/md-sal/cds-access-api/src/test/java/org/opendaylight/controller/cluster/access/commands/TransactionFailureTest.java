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

public class TransactionFailureTest extends AbstractRequestFailureTest<TransactionFailure> {
    private static final TransactionFailure OBJECT = new TransactionFailure(TRANSACTION_IDENTIFIER, 0, CAUSE);

    @Override
    TransactionFailure object() {
        return OBJECT;
    }

    @Test
    public void cloneAsVersionTest() throws Exception {
        final TransactionFailure clone = OBJECT.cloneAsVersion(ABIVersion.current());
        Assert.assertEquals(OBJECT, clone);
    }
}