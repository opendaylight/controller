/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionChain;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainClosedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionFactory;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import scala.concurrent.Future;

/**
 * A chain of {@link TransactionProxy}s. Beside tracking transactions it also tracks
 * the 
 */
final class ShardedTransactionChain extends ComponentFactory implements DOMStoreTransactionChain {
    private static abstract class State {
        abstract boolean isReady();
        abstract List<Future<Object>> getPreviousReadyFutures();
    }

    private static class Allocated extends State {
        private final TransactionProxy transaction;

        Allocated(TransactionProxy transaction) {
            this.transaction = transaction;
        }

        @Override
        boolean isReady() {
            return transaction.isReady();
        }

        @Override
        List<Future<Object>> getPreviousReadyFutures() {
            return transaction.getReadyFutures();
        }
    }

    private static abstract class AbstractDefaultState extends State {
        @Override
        List<Future<Object>> getPreviousReadyFutures() {
            return Collections.emptyList();
        }
    }

    private static final State IDLE_STATE = new AbstractDefaultState() {
        @Override
        boolean isReady() {
            return true;
        }
    };

    private static final State CLOSED_STATE = new AbstractDefaultState() {
        @Override
        boolean isReady() {
            throw new TransactionChainClosedException("Transaction chain has been closed");
        }
    };

    private static final AtomicInteger CHAIN_COUNTER = new AtomicInteger();
    // FIXME: this needs to be invalidated somehow
    private final Map<String, LocalTransactionChain> localChains = new HashMap<>();
    private final String transactionChainId;
    private final ComponentFactory parent;
    private volatile State currentState = IDLE_STATE;
    private long counter = 0;

    ShardedTransactionChain(final ComponentFactory parent) {
        super(parent.getActorContext());
        transactionChainId = parent.getActorContext().getCurrentMemberName() + "-txn-chain-" + CHAIN_COUNTER.incrementAndGet();
        this.parent = Preconditions.checkNotNull(parent);
    }

    public String getTransactionChainId() {
        return transactionChainId;
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        State localState = currentState;
        checkReadyState(localState);

        // FIXME: why were we leaking these?
//        return new ChainedTransactionProxy(actorContext, TransactionType.READ_ONLY,
//            transactionChainId, localState.getPreviousReadyFutures());
        // FIXME: increment counter
        return new TransactionProxy(this, TransactionType.READ_ONLY);
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        getActorContext().acquireTxCreationPermit();
        return allocateWriteTransaction(TransactionType.READ_WRITE);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        getActorContext().acquireTxCreationPermit();
        return allocateWriteTransaction(TransactionType.WRITE_ONLY);
    }

    @Override
    public void close() {
        currentState = CLOSED_STATE;

        // Send a close transaction chain request to each and every shard
        getActorContext().broadcast(new CloseTransactionChain(transactionChainId).toSerializable());
    }

    private TransactionProxy allocateWriteTransaction(TransactionType type) {
        State localState = currentState;

        checkReadyState(localState);

        // Pass the ready Futures from the previous Tx.
//        ChainedTransactionProxy txProxy = new ChainedTransactionProxy(getActorContext(), type,
//                transactionChainId, localState.getPreviousReadyFutures());
        // FIXME: increment counter
        final TransactionProxy ret = new TransactionProxy(this, type);
        currentState = new Allocated(ret);
        return ret;
    }

    private static void checkReadyState(final State state) {
        Preconditions.checkState(state.isReady(), "Previous transaction is not ready yet");
    }
    
    long currentCounter() {
        return counter;
    }

    @Override
    protected DOMStoreTransactionFactory factoryForShard(String shardName, DataTree dataTree) {
        LocalTransactionChain ret = localChains.get(shardName);
        if (ret == null) {
            ret = new LocalTransactionChain(this, shardName, dataTree);
            localChains.put(shardName, ret);
        }

        return ret;
    }

    @Override
    protected Future<PrimaryShardInfo> findPrimaryShard(String shardName) {
        // FIXME: hijack outstanding ready transactions first
        return parent.findPrimaryShard(shardName);
    }
}
