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
import org.opendaylight.controller.cluster.datastore.messages.DeleteData;
import org.opendaylight.controller.cluster.datastore.messages.MergeData;
import org.opendaylight.controller.cluster.datastore.messages.WriteData;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Implementation of TransactionContextImpl used when talking to a pre-Lithium controller that doesn't
 * support the BatchedModifications message.
 *
 * @author Thomas Pantelis
 */
class LegacyTransactionContextImpl extends TransactionContextImpl {

    LegacyTransactionContextImpl(String transactionPath, ActorSelection actor, TransactionIdentifier identifier,
            ActorContext actorContext, SchemaContext schemaContext, boolean isTxActorLocal,
            short remoteTransactionVersion, OperationCompleter operationCompleter) {
        super(transactionPath, actor, identifier, actorContext, schemaContext, isTxActorLocal,
                remoteTransactionVersion,  operationCompleter);
    }

    @Override
    public void deleteData(YangInstanceIdentifier path) {
        recordedOperationFutures.add(executeOperationAsync(
                new DeleteData(path, getRemoteTransactionVersion())));
    }

    @Override
    public void mergeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        recordedOperationFutures.add(executeOperationAsync(
                new MergeData(path, data, getRemoteTransactionVersion())));
    }

    @Override
    public void writeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        recordedOperationFutures.add(executeOperationAsync(
                new WriteData(path, data, getRemoteTransactionVersion())));
    }
}
