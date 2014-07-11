/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.CheckedFuture;

/**
 * NormalizedNode implementation of {@link org.opendaylight.controller.md.sal.common.api.data.TransactionChain} which is backed
 * by several {@link DOMStoreTransactionChain} differentiated by provided
 * {@link org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType} type.
 *
 */
public class DOMDataBrokerTransactionChainImpl extends AbstractDOMForwardedTransactionFactory<DOMStoreTransactionChain>
        implements DOMTransactionChain, DOMDataCommitErrorListener {

    private static final Logger LOG = LoggerFactory.getLogger(DOMDataBrokerTransactionChainImpl.class);
    private final DOMDataCommitExecutor coordinator;
    private final TransactionChainListener listener;
    private final long chainId;
    private final AtomicLong txNum = new AtomicLong();
    @GuardedBy("this")
    private boolean failed = false;

    /**
     *
     * @param chainId
     *            ID of transaction chain
     * @param chains
     *            Backing {@link DOMStoreTransactionChain}s.
     * @param coordinator
     *            Commit Coordinator which should be used to coordinate commits
     *            of transaction
     *            produced by this chain.
     * @param listener
     *            Listener, which listens on transaction chain events.
     * @throws NullPointerException
     *             If any of arguments is null.
     */
    public DOMDataBrokerTransactionChainImpl(final long chainId,
            final ImmutableMap<LogicalDatastoreType, DOMStoreTransactionChain> chains,
            final DOMDataCommitExecutor coordinator, final TransactionChainListener listener) {
        super(chains);
        this.chainId = chainId;
        this.coordinator = Preconditions.checkNotNull(coordinator);
        this.listener = Preconditions.checkNotNull(listener);
    }

    @Override
    protected Object newTransactionIdentifier() {
        return "DOM-CHAIN-" + chainId + "-" + txNum.getAndIncrement();
    }

    @Override
    public synchronized CheckedFuture<Void,TransactionCommitFailedException> submit(
            final DOMDataWriteTransaction transaction, final Iterable<DOMStoreThreePhaseCommitCohort> cohorts) {
        return coordinator.submit(transaction, cohorts, Optional.<DOMDataCommitErrorListener> of(this));
    }

    @Override
    public synchronized void close() {
        super.close();
        for (DOMStoreTransactionChain subChain : getTxFactories().values()) {
            subChain.close();
        }

        if (!failed) {
            LOG.debug("Transaction chain {} successfully finished.", this);
            listener.onTransactionChainSuccessful(this);
        }
    }

    @Override
    public synchronized void onCommitFailed(final DOMDataWriteTransaction tx, final Throwable cause) {
        failed = true;
        LOG.debug("Transaction chain {} failed.", this, cause);
        listener.onTransactionChainFailed(this, tx, cause);
    }
}
