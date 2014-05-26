package org.opendaylight.controller.md.sal.dom.broker.impl;

import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;

public class DOMBrokerTransactionChainImpl extends AbstractDOMForwardedTransactionFactory<DOMStoreTransactionChain> implements
        DOMTransactionChain, DOMDataCommitErrorListener {

    private final DOMDataCommitCoordinatorImpl coordinator;
    private final TransactionChainListener listener;
    private final long chainId;
    private final AtomicLong txNum = new AtomicLong();
    private boolean failed = false;

    public DOMBrokerTransactionChainImpl(final long chainId,final ImmutableMap<LogicalDatastoreType, DOMStoreTransactionChain> chains,
            final DOMDataCommitCoordinatorImpl coordinator, final TransactionChainListener listener) {
        super(chains);
        this.chainId = chainId;
        this.coordinator = Preconditions.checkNotNull(coordinator);
        this.listener = Preconditions.checkNotNull(listener);

    }

    @Override
    synchronized Object newTransactionIdentifier() {
        return "DOM-CHAIN-" + chainId + "-" + txNum.getAndIncrement();
    }

    @Override
    ListenableFuture<RpcResult<TransactionStatus>> submit(final DOMDataWriteTransaction transaction,
            final Iterable<DOMStoreThreePhaseCommitCohort> cohorts) {
        return coordinator.submit(transaction, cohorts, Optional.<DOMDataCommitErrorListener>of(this));
    }

    @Override
    public void close() {
        for(DOMStoreTransactionChain subChain : getTxFactories().values() ) {
            subChain.close();
        }

        if(!failed) {
            listener.onTransactionChainSuccessful(this);
        }
    }

    @Override
    synchronized public void onCommitFailed(final DOMDataWriteTransaction tx, final Throwable cause) {
        failed = true;
        listener.onTransactionChainFailed(this, tx, cause);
    }
}
