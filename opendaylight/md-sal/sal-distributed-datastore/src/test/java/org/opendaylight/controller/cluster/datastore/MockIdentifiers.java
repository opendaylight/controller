/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;

public final class MockIdentifiers {
    private static final class MockFrontendType implements FrontendType {
        private static final long serialVersionUID = 1L;
        private final String className;

        public MockFrontendType(final Class<?> clazz) {
            className = clazz.getName();
        }

        @Override
        public int hashCode() {
            return className.hashCode();
        }

        @Override
        public boolean equals(final Object o) {
            return this == o || (o instanceof MockFrontendType && className.equals(((MockFrontendType)o).className));
        }
    }

    public static ClientIdentifier<?> clientIdentifier(final Class<?> clazz, final String memberName) {
        // TODO Auto-generated method stub
        return ClientIdentifier.create(FrontendIdentifier.create(MemberName.forName(memberName),
            new MockFrontendType(clazz)), 0);
    };
}
