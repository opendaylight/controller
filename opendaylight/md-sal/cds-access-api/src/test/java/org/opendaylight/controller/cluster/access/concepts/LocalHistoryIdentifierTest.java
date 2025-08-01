/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

class LocalHistoryIdentifierTest extends AbstractIdentifierTest<LocalHistoryIdentifier> {
    private static final FrontendIdentifier FRONTEND =
            new FrontendIdentifier(MemberName.forName("test"), FrontendIdentifierTest.ONE_FRONTEND_TYPE);
    private static final ClientIdentifier CLIENT = new ClientIdentifier(FRONTEND, 0);

    private static final LocalHistoryIdentifier OBJECT = new LocalHistoryIdentifier(CLIENT, 0);
    private static final LocalHistoryIdentifier DIFFERENT_OBJECT = new LocalHistoryIdentifier(CLIENT, 1);
    private static final LocalHistoryIdentifier EQUAL_OBJECT = new LocalHistoryIdentifier(CLIENT, 0);

    @Override
    LocalHistoryIdentifier object() {
        return OBJECT;
    }

    @Override
    LocalHistoryIdentifier differentObject() {
        return DIFFERENT_OBJECT;
    }

    @Override
    LocalHistoryIdentifier equalObject() {
        return EQUAL_OBJECT;
    }

    @Override
    int expectedSize() {
        return 95;
    }
}
