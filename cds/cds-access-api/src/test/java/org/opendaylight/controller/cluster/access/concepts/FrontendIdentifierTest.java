/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

class FrontendIdentifierTest extends AbstractIdentifierTest<FrontendIdentifier> {
    static final FrontendType ONE_FRONTEND_TYPE = FrontendType.forName("one");
    static final FrontendType OTHER_FRONTEND_TYPE = FrontendType.forName("two");

    private static final MemberName MEMBER = MemberName.forName("test");
    private static final FrontendIdentifier OBJECT = new FrontendIdentifier(MEMBER, ONE_FRONTEND_TYPE);
    private static final FrontendIdentifier DIFFERENT_OBJECT = new FrontendIdentifier(MEMBER, OTHER_FRONTEND_TYPE);
    private static final FrontendIdentifier EQUAL_OBJECT = new FrontendIdentifier(MEMBER, ONE_FRONTEND_TYPE);

    @Override
    FrontendIdentifier object() {
        return OBJECT;
    }

    @Override
    FrontendIdentifier differentObject() {
        return DIFFERENT_OBJECT;
    }

    @Override
    FrontendIdentifier equalObject() {
        return EQUAL_OBJECT;
    }

    @Override
    int expectedSize() {
        return 93;
    }
}
