/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * Context for a write-only transaction.
 *
 * @author Thomas Pantelis
 */
public class WriteOnlyTransactionContextImpl extends TransactionContextImpl {
    private static final Logger LOG = LoggerFactory.getLogger(WriteOnlyTransactionContextImpl.class);

    public WriteOnlyTransactionContextImpl(ActorSelection actor, TransactionIdentifier identifier,
            String transactionChainId, ActorContext actorContext, SchemaContext schemaContext, boolean isTxActorLocal,
            short remoteTransactionVersion, OperationCompleter operationCompleter) {
        super(actor, identifier, transactionChainId, actorContext, schemaContext, isTxActorLocal,
                remoteTransactionVersion, operationCompleter);
    }

    @Override
    public Future<ActorSelection> readyTransaction() {
        LOG.debug("Tx {} readyTransaction called with {} previous recorded operations pending",
                identifier, recordedOperationFutures.size());

        // Send the remaining batched modifications if any.

        Future<Object> lastModificationsFuture = sendBatchedModifications(true);

        return combineRecordedOperationsFutures(lastModificationsFuture);
    }
}
