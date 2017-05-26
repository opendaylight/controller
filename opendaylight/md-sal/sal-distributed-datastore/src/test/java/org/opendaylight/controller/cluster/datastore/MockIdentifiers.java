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
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

public final class MockIdentifiers {
    public static ClientIdentifier clientIdentifier(final Class<?> clazz, final String memberName) {
        return ClientIdentifier.create(FrontendIdentifier.create(MemberName.forName(memberName),
            FrontendType.forName(clazz.getSimpleName())), 0);
    }

    public static LocalHistoryIdentifier historyIdentifier(final Class<?> clazz, final String memberName) {
        return new LocalHistoryIdentifier(clientIdentifier(clazz, memberName), 0);
    }

    public static TransactionIdentifier transactionIdentifier(final Class<?> clazz, final String memberName) {
        return new TransactionIdentifier(historyIdentifier(clazz, memberName), 0);
    }
}
