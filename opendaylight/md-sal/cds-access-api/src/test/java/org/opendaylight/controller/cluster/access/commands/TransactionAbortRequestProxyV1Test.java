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

public class TransactionAbortRequestProxyV1Test extends AbstractRequestProxyTest<TransactionAbortRequestProxyV1> {
    private static final TransactionAbortRequest REQUEST = new TransactionAbortRequest(
            TRANSACTION_IDENTIFIER, 0, ACTOR_REF);
    private static final TransactionAbortRequestProxyV1 OBJECT = new TransactionAbortRequestProxyV1(REQUEST);

    @Override
    public TransactionAbortRequestProxyV1 object() {
        return OBJECT;
    }

    @Test
    public void createRequestTest() {
        final Request request = object().createRequest(TRANSACTION_IDENTIFIER, 0, ACTOR_REF);
        Assert.assertNotNull(request);
    }
}