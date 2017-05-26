/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.junit.Assert;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

public class AbortLocalTransactionRequestTest
        extends AbstractLocalTransactionRequestTest<AbortLocalTransactionRequest> {
    private static final FrontendIdentifier FRONTEND = FrontendIdentifier.create(
            MemberName.forName("test"), FrontendType.forName("one"));
    private static final ClientIdentifier CLIENT = ClientIdentifier.create(FRONTEND, 0);
    private static final LocalHistoryIdentifier HISTORY = new LocalHistoryIdentifier(CLIENT, 0);
    private static final TransactionIdentifier TRANSACTION = new TransactionIdentifier(HISTORY, 0);

    private static final AbortLocalTransactionRequest OBJECT = new AbortLocalTransactionRequest(TRANSACTION, ACTOR_REF);

    @Override
    protected AbortLocalTransactionRequest object() {
        return OBJECT;
    }

    @Override
    protected void doAdditionalAssertions(final Object deserialize) {
        Assert.assertTrue(deserialize instanceof AbortLocalTransactionRequest);
        Assert.assertEquals(OBJECT.getReplyTo(), ((AbortLocalTransactionRequest) deserialize).getReplyTo());
    }
}