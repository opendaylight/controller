/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.compat;

import akka.actor.ActorSelection;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.OperationCompleter;
import org.opendaylight.controller.cluster.datastore.TransactionContextImpl;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.DeleteData;
import org.opendaylight.controller.cluster.datastore.messages.MergeData;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.WriteData;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * Implementation of TransactionContextImpl used when talking to a pre-Lithium controller that doesn't
 * support the BatchedModifications message.
 *
 * @author Thomas Pantelis
 */
public class PreLithiumTransactionContextImpl extends TransactionContextImpl {
    private static final Logger LOG = LoggerFactory.getLogger(PreLithiumTransactionContextImpl.class);

    private final String transactionPath;

    public PreLithiumTransactionContextImpl(String transactionPath, ActorSelection actor, TransactionIdentifier identifier,
            ActorContext actorContext, boolean isTxActorLocal,
            short remoteTransactionVersion, OperationCompleter operationCompleter) {
        super(actor, identifier, actorContext, isTxActorLocal, remoteTransactionVersion, operationCompleter);
        this.transactionPath = transactionPath;
    }

    @Override
    public void deleteData(YangInstanceIdentifier path) {
        executeOperationAsync(new DeleteData(path, getRemoteTransactionVersion()));
    }

    @Override
    public void mergeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        executeOperationAsync(new MergeData(path, data, getRemoteTransactionVersion()));
    }

    @Override
    public void writeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        executeOperationAsync(new WriteData(path, data, getRemoteTransactionVersion()));
    }

    @Override
    public Future<ActorSelection> readyTransaction() {
        LOG.debug("Tx {} readyTransaction called", getIdentifier());

        // Send the ReadyTransaction message to the Tx actor.

        Future<Object> lastReplyFuture = executeOperationAsync(ReadyTransaction.INSTANCE);

        return transformReadyReply(lastReplyFuture);
    }

    @Override
    protected String extractCohortPathFrom(ReadyTransactionReply readyTxReply) {
        // In base Helium we used to return the local path of the actor which represented
        // a remote ThreePhaseCommitCohort. The local path would then be converted to
        // a remote path using this resolvePath method. To maintain compatibility with
        // a Helium node we need to continue to do this conversion.
        // At some point in the future when upgrades from Helium are not supported
        // we could remove this code to resolvePath and just use the cohortPath as the
        // resolved cohortPath
        if(getRemoteTransactionVersion() < DataStoreVersions.HELIUM_1_VERSION) {
            return getActorContext().resolvePath(transactionPath, readyTxReply.getCohortPath());
        }

        return readyTxReply.getCohortPath();
    }

    @Override
    public boolean supportsDirectCommit() {
        return false;
    }

    @Override
    public Future<Object> directCommit() {
        throw new UnsupportedOperationException("directCommit is not supported for " + getClass());
    }
}
