/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl.legacy.sharded.adapter;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

class ShardedDOMDataBrokerDelegatingTransactionChain implements DOMTransactionChain, DOMTransactionChainListener {
    private final org.opendaylight.mdsal.dom.api.DOMTransactionChain txChainDelegate;
    private final SchemaContext schemaContext;
    private final TransactionChainListener txChainListener;
    private final Object txChainIdentifier;
    private final AtomicLong txNum = new AtomicLong();

    private final Map<Object, AsyncTransaction<?, ?>> transactionMap;

    ShardedDOMDataBrokerDelegatingTransactionChain(final Object txChainIdentifier,
                                                          final SchemaContext schemaContext,
                                                          final org.opendaylight.mdsal.dom.api.DOMDataBroker
                                                                  brokerDelegate,
                                                          final TransactionChainListener txChainListener) {
        requireNonNull(brokerDelegate);
        this.schemaContext = requireNonNull(schemaContext);
        this.txChainIdentifier = requireNonNull(txChainIdentifier);
        this.txChainListener = requireNonNull(txChainListener);
        this.txChainDelegate = brokerDelegate.createTransactionChain(this);
        transactionMap = Maps.newHashMap();
    }

    @Override
    public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        final DOMDataTreeReadTransaction readTxDelegate = txChainDelegate.newReadOnlyTransaction();
        final DOMDataReadOnlyTransaction readTx = new ShardedDOMDataBrokerDelegatingReadTransaction(
                newTransactionIdentifier(), readTxDelegate);
        transactionMap.put(readTxDelegate.getIdentifier(), readTx);

        return readTx;
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        final Object readWriteTxId = newTransactionIdentifier();
        final DOMDataTreeReadTransaction readTxDelegate = txChainDelegate.newReadOnlyTransaction();
        final DOMDataReadOnlyTransaction readTx = new ShardedDOMDataBrokerDelegatingReadTransaction(readWriteTxId,
                                                                                                    readTxDelegate);

        final DOMDataTreeWriteTransaction writeTxDelegate = txChainDelegate.newWriteOnlyTransaction();
        final DOMDataWriteTransaction writeTx = new ShardedDOMDataBrokerDelegatingWriteTransaction(readWriteTxId,
                                                                                                   writeTxDelegate);

        final DOMDataReadWriteTransaction readWriteTx = new ShardedDOMDataBrokerDelegatingReadWriteTransaction(
                readWriteTxId, schemaContext, readTx, writeTx);
        transactionMap.put(readTxDelegate.getIdentifier(), readWriteTx);
        transactionMap.put(writeTxDelegate.getIdentifier(), readWriteTx);

        return readWriteTx;
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        final DOMDataTreeWriteTransaction writeTxDelegate = txChainDelegate.newWriteOnlyTransaction();
        final DOMDataWriteTransaction writeTx = new ShardedDOMDataBrokerDelegatingWriteTransaction(
                newTransactionIdentifier(), writeTxDelegate);
        transactionMap.put(writeTxDelegate.getIdentifier(), writeTx);

        return writeTx;
    }

    @Override
    public void close() {
        txChainDelegate.close();
    }

    @Override
    public void onTransactionChainFailed(org.opendaylight.mdsal.dom.api.DOMTransactionChain chain,
            DOMDataTreeTransaction transaction, Throwable cause) {
        txChainListener.onTransactionChainFailed(this, transactionFromDelegate(transaction.getIdentifier()), cause);
    }

    @Override
    public void onTransactionChainSuccessful(org.opendaylight.mdsal.dom.api.DOMTransactionChain chain) {
        txChainListener.onTransactionChainSuccessful(this);
    }

    private AsyncTransaction<?, ?> transactionFromDelegate(final Object delegateId) {
        Preconditions.checkState(transactionMap.containsKey(delegateId),
                                 "Delegate transaction {} is not present in transaction chain history", delegateId);
        return transactionMap.get(delegateId);
    }

    private Object newTransactionIdentifier() {
        return "DOM-CHAIN-" + txChainIdentifier + "-" + txNum.getAndIncrement();
    }
}
