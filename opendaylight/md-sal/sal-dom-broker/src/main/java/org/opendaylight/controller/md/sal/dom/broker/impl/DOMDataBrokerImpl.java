/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import static com.google.common.base.Preconditions.checkState;

import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class DOMDataBrokerImpl extends AbstractDOMForwardedTransactionFactory<DOMStore> implements DOMDataBroker,
        AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DOMDataBrokerImpl.class);

    private final DOMDataCommitCoordinatorImpl coordinator;
    private final AtomicLong txNum = new AtomicLong();
    private final AtomicLong chainNum = new AtomicLong();

    public DOMDataBrokerImpl(final ImmutableMap<LogicalDatastoreType, DOMStore> datastores,
            final ListeningExecutorService executor) {
        super(datastores);
        this.coordinator = new DOMDataCommitCoordinatorImpl(executor);
    }

    @Override
    protected Object newTransactionIdentifier() {
        return "DOM-" + txNum.getAndIncrement();
    }

    @Override
    public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(final LogicalDatastoreType store,
            final YangInstanceIdentifier path, final DOMDataChangeListener listener, final DataChangeScope triggeringScope) {

        DOMStore potentialStore = getTxFactories().get(store);
        checkState(potentialStore != null, "Requested logical data store is not available.");
        return potentialStore.registerChangeListener(path, listener, triggeringScope);
    }

    @Override
    public DOMTransactionChain createTransactionChain(final TransactionChainListener listener) {
        ImmutableMap.Builder<LogicalDatastoreType, DOMStoreTransactionChain> backingChainsBuilder = ImmutableMap
                .builder();
        for (Entry<LogicalDatastoreType, DOMStore> entry : getTxFactories().entrySet()) {
            backingChainsBuilder.put(entry.getKey(), entry.getValue().createTransactionChain());
        }
        long chainId = chainNum.getAndIncrement();
        ImmutableMap<LogicalDatastoreType, DOMStoreTransactionChain> backingChains = backingChainsBuilder.build();
        LOG.debug("Transactoin chain {} created with listener {}, backing store chains {}", chainId, listener,
                backingChains);
        return new DOMDataBrokerTransactionChainImpl(chainId, backingChains, coordinator, listener);

    }

    @Override
    public CheckedFuture<Void,TransactionCommitFailedException> submit(final DOMDataWriteTransaction transaction,
            final Iterable<DOMStoreThreePhaseCommitCohort> cohorts) {
        LOG.debug("Transaction: {} submitted with cohorts {}.", transaction.getIdentifier(), cohorts);
        return coordinator.submit(transaction, cohorts, Optional.<DOMDataCommitErrorListener> absent());
    }

}
