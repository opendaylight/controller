/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Optional;
import java.util.SortedSet;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.AbstractRead;
import org.opendaylight.yangtools.concepts.AbstractSimpleIdentifiable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

abstract class TransactionContext extends AbstractSimpleIdentifiable<TransactionIdentifier> {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionContext.class);

    private final short transactionVersion;

    private long modificationCount = 0;
    private boolean handOffComplete;

    TransactionContext(final TransactionIdentifier transactionIdentifier) {
        this(transactionIdentifier, DataStoreVersions.CURRENT_VERSION);
    }

    TransactionContext(final TransactionIdentifier transactionIdentifier, final short transactionVersion) {
        super(transactionIdentifier);
        this.transactionVersion = transactionVersion;
    }

    final short getTransactionVersion() {
        return transactionVersion;
    }

    final void incrementModificationCount() {
        modificationCount++;
    }

    final void logModificationCount() {
        LOG.debug("Total modifications on Tx {} = [ {} ]", getIdentifier(), modificationCount);
    }

    /**
     * Invoked by {@link AbstractTransactionContextWrapper} when it has finished handing
     * off operations to this context. From this point on, the context is responsible
     * for throttling operations.
     *
     * <p>
     * Implementations can rely on the wrapper calling this operation in a synchronized
     * block, so they do not need to ensure visibility of this state transition themselves.
     */
    final void operationHandOffComplete() {
        handOffComplete = true;
    }

    final boolean isOperationHandOffComplete() {
        return handOffComplete;
    }

    /**
     * A TransactionContext that uses operation limiting should return true else false.
     *
     * @return true if operation limiting is used, false otherwise
     */
    boolean usesOperationLimiting() {
        return false;
    }

    abstract void executeDelete(YangInstanceIdentifier path, Boolean havePermit);

    abstract void executeMerge(YangInstanceIdentifier path, NormalizedNode data, Boolean havePermit);

    abstract void executeWrite(YangInstanceIdentifier path, NormalizedNode data, Boolean havePermit);

    abstract <T> void executeRead(AbstractRead<T> readCmd, SettableFuture<T> proxyFuture, Boolean havePermit);

    abstract Future<ActorSelection> readyTransaction(Boolean havePermit,
            Optional<SortedSet<String>> participatingShardNames);

    abstract Future<Object> directCommit(Boolean havePermit);

    abstract void closeTransaction();
}
