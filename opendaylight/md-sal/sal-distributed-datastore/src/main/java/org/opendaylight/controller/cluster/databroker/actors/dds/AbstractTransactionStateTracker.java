/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;

/**
 * Class tracking messages exchanged with a particular backend shard.
 *
 * This class is not safe to access from multiple application threads, as is usual for transactions. Internal state
 * transitions coming from interactions with backend are expected to be thread-safe.
 *
 * This class interacts with the queueing mechanism in ClientActorBehavior, hence once we arrive at a decision
 * to use either a local or remote implementation, we are stuck with it. We can re-evaluate on the next transaction.
 *
 * @author Robert Varga
 */
abstract class AbstractTransactionStateTracker implements Identifiable<TransactionIdentifier> {
    private final DistributedDataStoreClientBehavior client;

    AbstractTransactionStateTracker(final DistributedDataStoreClientBehavior client) {
        this.client = Preconditions.checkNotNull(client);
    }

    /**
     * Instantiate a new tracker for a transaction. This method bases its decision on which implementation to use
     * based on provided {@link ShardBackendInfo}. If no information is present, it will choose the remote
     * implementation, which is fine, as the queueing logic in ClientActorBehavior will hold on to the requests until
     * the backend is located.
     *
     * @param client Client behavior
     * @param historyId Local history identifier
     * @param transactionId Transaction identifier
     * @param backend Optional backend identifier
     * @return A new state tracker
     */
    static AbstractTransactionStateTracker create(final DistributedDataStoreClientBehavior client,
            final LocalHistoryIdentifier historyId, final long transactionId,
            final @Nullable ShardBackendInfo backend) {

        final java.util.Optional<DataTree> dataTree = backend == null ? java.util.Optional.empty()
                : backend.getDataTree();

        final TransactionIdentifier identifier = new TransactionIdentifier(historyId, transactionId);
        if (dataTree.isPresent()) {
            return new LocalTransactionStateTracker(client, identifier, dataTree.get());
        } else {
            return new RemoteTransactionStateTracker(client, identifier);
        }
    }

    final DistributedDataStoreClientBehavior client() {
        return client;
    }

    abstract void delete(final YangInstanceIdentifier path);

    abstract void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data);

    abstract void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data);

    abstract CompletionStage<Boolean> exists(final YangInstanceIdentifier path);

    abstract CompletionStage<Optional<NormalizedNode<?, ?>>> read(final YangInstanceIdentifier path);

    // FIXME: add commit call, which will result in a Response coming from the backend. The coordination part
    //        will be left to the caller, so this object can be garbage-collected.

    abstract void abort();
}
