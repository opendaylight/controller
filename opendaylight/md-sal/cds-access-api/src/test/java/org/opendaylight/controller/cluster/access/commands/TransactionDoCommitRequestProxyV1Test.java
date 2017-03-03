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
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestProxyTest;
import org.opendaylight.controller.cluster.access.concepts.Request;

public class TransactionDoCommitRequestProxyV1Test extends AbstractRequestProxyTest<TransactionDoCommitRequestProxyV1> {

    private static final TransactionDoCommitRequest REQUEST = new TransactionDoCommitRequest(
            TRANSACTION_IDENTIFIER, 0, ACTOR_REF);

    private static final TransactionDoCommitRequestProxyV1 OBJECT = new TransactionDoCommitRequestProxyV1(REQUEST);

    @Override
    public TransactionDoCommitRequestProxyV1 object() {
        return OBJECT;
    }

    @Test
    public void createRequestTest() {
        final Request purgeRequest = object().createRequest(TRANSACTION_IDENTIFIER, 0, ACTOR_REF);
        Assert.assertNotNull(purgeRequest);
    }
}