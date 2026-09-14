/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

class TransactionIdentifierTest extends AbstractIdentifierTest<TransactionIdentifier> {
    private static final FrontendIdentifier FRONTEND =
            new FrontendIdentifier(MemberName.forName("test"), FrontendIdentifierTest.ONE_FRONTEND_TYPE);
    private static final ClientIdentifier CLIENT = new ClientIdentifier(FRONTEND, 0);
    private static final LocalHistoryIdentifier HISTORY = new LocalHistoryIdentifier(CLIENT, 0);

    private static final TransactionIdentifier OBJECT = new TransactionIdentifier(HISTORY, 0);
    private static final TransactionIdentifier DIFFERENT_OBJECT = new TransactionIdentifier(HISTORY, 1);
    private static final TransactionIdentifier EQUAL_OBJECT = new TransactionIdentifier(HISTORY, 0);

    @Override
    TransactionIdentifier object() {
        return OBJECT;
    }

    @Override
    TransactionIdentifier differentObject() {
        return DIFFERENT_OBJECT;
    }

    @Override
    TransactionIdentifier equalObject() {
        return EQUAL_OBJECT;
    }

    @Override
    int expectedSize() {
        return 96;
    }
}
