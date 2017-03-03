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

public class TransactionPurgeRequestProxyV1Test extends AbstractRequestProxyTest<TransactionPurgeRequestProxyV1> {
    private static final TransactionPurgeRequest REQUEST = new TransactionPurgeRequest(
            TRANSACTION_IDENTIFIER, 0, ACTOR_REF);
    private static final TransactionPurgeRequestProxyV1 OBJECT = new TransactionPurgeRequestProxyV1(REQUEST);

    @Override
    public TransactionPurgeRequestProxyV1 object() {
        return OBJECT;
    }

    @Test
    public void createRequestTest() {
        final Request request = object().createRequest(TRANSACTION_IDENTIFIER, 0, ACTOR_REF);
        Assert.assertNotNull(request);
    }
}