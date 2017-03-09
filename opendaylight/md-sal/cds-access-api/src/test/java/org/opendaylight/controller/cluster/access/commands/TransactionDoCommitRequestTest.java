/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.apache.commons.lang.SerializationUtils;
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
    public void cloneAsVersion() throws Exception {
        final TransactionDoCommitRequest clone = OBJECT.cloneAsVersion(ABIVersion.BORON);
        Assert.assertEquals(OBJECT, clone);
    }

    @Test
    public void serializationTest() {
        final Object deserialize = SerializationUtils.clone(object());

        Assert.assertEquals(object().getTarget(), ((TransactionDoCommitRequest) deserialize).getTarget());
        Assert.assertEquals(object().getVersion(), ((TransactionDoCommitRequest) deserialize).getVersion());
        Assert.assertEquals(object().getSequence(), ((TransactionDoCommitRequest) deserialize).getSequence());
    }
}