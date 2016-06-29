/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;
import javax.print.attribute.standard.RequestingUserName;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.RequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frontend state as observed by the shard leader. this class is responsible for tracking generations and sequencing
 * in the frontend/backend conversation.
 *
 * @author Robert Varga
 */
@NotThreadSafe
final class FrontendState {
    private static final Logger LOG = LoggerFactory.getLogger(FrontendState.class);

    private final Map<TransactionIdentifier, FrontendTransaction> transactions = new HashMap<>();
    private final ClientIdentifier clientId;
    private final String persistenceId;

    FrontendState(final String persistenceId, final ClientIdentifier clientId) {
        this.persistenceId = Preconditions.checkNotNull(persistenceId);
        this.clientId = Preconditions.checkNotNull(clientId);
    }

    long getGeneration() {
        return clientId.getGeneration();
    }

    void handleTransactionRequest(final TransactionRequest<?> request) {
        final TransactionIdentifier txId = request.getTarget();

        final FrontendTransaction existing = transactions.get(txId);
        if (existing == null) {
            if (request.getSequence() != 0) {
                LOG.debug("{}: no transaction state present, unexpected request {}", persistenceId, request);
                RequestUtil.sendFailure(request, cause);
                return;
            }
            
            
        }



        final LocalHistoryIdentifier historyId = txId.getHistoryId();

        // If this is a
        if (historyId.getHistoryId() != 0) {


        }



        // TODO Auto-generated method stub

    }

    void retire() {
        // FIXME: flush all state
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(FrontendState.class).add("clientId", clientId).toString();
    }
}
