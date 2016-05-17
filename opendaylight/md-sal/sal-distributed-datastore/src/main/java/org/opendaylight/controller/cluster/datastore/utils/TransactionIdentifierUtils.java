/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

public final class TransactionIdentifierUtils {
    private TransactionIdentifierUtils() {
        throw new UnsupportedOperationException();
    }

    public static String actorNameFor(final TransactionIdentifier<?> txId) {
        final LocalHistoryIdentifier<?> historyId = txId.getHistoryId();
        final ClientIdentifier<?> clientId = historyId.getClienId();
        final FrontendIdentifier<?> frontendId = clientId.getFrontendId();

        final StringBuilder sb = new StringBuilder();
        sb.append(frontendId.getMemberName().getName()).append(':');
        sb.append(frontendId.getClientType().toSimpleString()).append('@');
        sb.append(clientId.getGeneration()).append(':');
        if (historyId.getHistoryId() != 0) {
            sb.append(historyId.getHistoryId()).append('-');
        }

        return sb.append(txId.getTransactionId()).toString();
    }
}
