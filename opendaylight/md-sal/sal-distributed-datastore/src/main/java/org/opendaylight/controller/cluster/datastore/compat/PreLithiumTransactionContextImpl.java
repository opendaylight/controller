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
import org.opendaylight.controller.cluster.datastore.OperationLimiter;
import org.opendaylight.controller.cluster.datastore.RemoteTransactionContext;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.DeleteData;
import org.opendaylight.controller.cluster.datastore.messages.MergeData;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.VersionedExternalizableMessage;
import org.opendaylight.controller.cluster.datastore.messages.WriteData;
import org.opendaylight.controller.cluster.datastore.modification.AbstractModification;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
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
@Deprecated
public class PreLithiumTransactionContextImpl extends RemoteTransactionContext {
    private static final Logger LOG = LoggerFactory.getLogger(PreLithiumTransactionContextImpl.class);

    private final String transactionPath;

    public PreLithiumTransactionContextImpl(TransactionIdentifier identifier, String transactionPath, ActorSelection actor,
            ActorContext actorContext, boolean isTxActorLocal,
            short remoteTransactionVersion, OperationLimiter limiter) {
        super(identifier, actor, actorContext, isTxActorLocal, remoteTransactionVersion, limiter);
        this.transactionPath = transactionPath;
    }

    @Override
    public void executeModification(AbstractModification modification) {
        final short remoteTransactionVersion = getRemoteTransactionVersion();
        final YangInstanceIdentifier path = modification.getPath();
        VersionedExternalizableMessage msg = null;

        if(modification instanceof DeleteModification) {
            msg = new DeleteData(path, remoteTransactionVersion);
        } else if(modification instanceof WriteModification) {
            final NormalizedNode<?, ?> data = ((WriteModification) modification).getData();

            // be sure to check for Merge before Write, since Merge is a subclass of Write
            if(modification instanceof MergeModification) {
                msg = new MergeData(path, data, remoteTransactionVersion);
            } else {
                msg = new WriteData(path, data, remoteTransactionVersion);
            }
        } else {
            LOG.error("Invalid modification type " + modification.getClass().getName());
        }

        if(msg != null) {
            executeOperationAsync(msg);
        }
    }

    @Override
    public Future<ActorSelection> readyTransaction() {
        LOG.debug("Tx {} readyTransaction called", getIdentifier());

        // Send the ReadyTransaction message to the Tx actor.

        Future<Object> lastReplyFuture = executeOperationAsync(ReadyTransaction.INSTANCE);

        return transformReadyReply(lastReplyFuture);
    }

    @Override
    protected Future<ActorSelection> transformReadyReply(final Future<Object> readyReplyFuture) {
        // In base Helium we used to return the local path of the actor which represented
        // a remote ThreePhaseCommitCohort. The local path would then be converted to
        // a remote path using this resolvePath method. To maintain compatibility with
        // a Helium node we need to continue to do this conversion.
        // At some point in the future when upgrades from Helium are not supported
        // we could remove this code to resolvePath and just use the cohortPath as the
        // resolved cohortPath
        if (getRemoteTransactionVersion() < DataStoreVersions.HELIUM_1_VERSION) {
            return PreLithiumTransactionReadyReplyMapper.transform(readyReplyFuture, getActorContext(), getIdentifier(), transactionPath);
        } else {
            return super.transformReadyReply(readyReplyFuture);
        }
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
