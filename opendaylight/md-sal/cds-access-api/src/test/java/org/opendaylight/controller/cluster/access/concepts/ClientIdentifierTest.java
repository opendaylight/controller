/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

class ClientIdentifierTest extends AbstractIdentifierTest<ClientIdentifier> {
    private static final FrontendIdentifier FRONTEND =
            new FrontendIdentifier(MemberName.forName("test"), FrontendIdentifierTest.ONE_FRONTEND_TYPE);

    private static final ClientIdentifier OBJECT = new ClientIdentifier(FRONTEND, 0);
    private static final ClientIdentifier DIFFERENT_OBJECT = new ClientIdentifier(FRONTEND, 1);
    private static final ClientIdentifier EQUAL_OBJECT = new ClientIdentifier(FRONTEND, 0);

    @Override
    ClientIdentifier object() {
        return OBJECT;
    }

    @Override
    ClientIdentifier differentObject() {
        return DIFFERENT_OBJECT;
    }

    @Override
    ClientIdentifier equalObject() {
        return EQUAL_OBJECT;
    }

    @Override
    int expectedSize() {
        return 94;
    }
}
