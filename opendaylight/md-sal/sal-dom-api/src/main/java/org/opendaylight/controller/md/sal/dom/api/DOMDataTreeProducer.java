/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import java.util.Collection;
import javax.annotation.Nonnull;

/**
 * A data producer context. It allows transactions to be submitted to the subtrees
 * specified at instantiation time. At any given time there may be a single transaction
 * open. It needs to be either submitted or cancelled before another one can be open.
 * Once a transaction is submitted, it will proceed to be committed asynchronously.
 *
 * Each instance has  an upper bound on the number of transactions which can be in-flight,
 * once that capacity is exceeded, an attempt to create a new transaction will block
 * until some transactions complete.
 *
 * Each {@link DOMDataTreeProducer} can be in two logical states, bound and unbound,
 * which define the lifecycle rules for when is it legal to create and submit transactions
 * in relationship with {@link DOMDataTreeListener} callbacks.
 *
 * When a producer is first created, it is unbound. In this state the producer can be
 * accessed by any application thread to allocate or submit transactions, as long as
 * the 'single open transaction' rule is maintained. The producer and any transaction
 * object MUST NOT be accessed, directly or indirectly, from a {@link DOMDataTreeListener}
 * callback.
 *
 * When a producer is referenced in a call to {@link DOMDataTreeService#registerListener(DOMDataTreeListener, java.util.Collection, boolean, java.util.Collection)},
 * an attempt will be made to bind the producer to the specified {@link DOMDataTreeListener}.
 * Such an attempt will fail the producer is already bound, or it has an open transaction.
 * Once bound, the producer can only be accessed from within the {@link DOMDataTreeListener}
 * callback on that particular instance. Any transaction which is not submitted by the
 * time the callback returns will be implicitly cancelled. A producer becomes unbound
 * when the listener it is bound to becomes unregistered.
 */
public interface DOMDataTreeProducer extends DOMDataTreeProducerFactory, AutoCloseable {
    /**
     * Allocate a new open transaction on this producer. Any and all transactions
     * previously allocated must have been either submitted or cancelled by the
     * time this method is invoked.
     *
     * @param barrier Indicates whether this transaction needs to be isolated from others.
     *                 If a transaction is not isolated, the backend is free to merge it
     *                 with preceding transactions such that they share fate. A transaction
     *                 may be merged with a succeeding transaction unless that other transaction
     *                 specifies it is a barrier.
     * @return A new {@link DOMDataWriteTransaction}
     * @throws {@link IllegalStateException} if a previous transaction was not closed.
     * @throws {@link IllegalThreadStateException} if the calling thread context does not
     *         match the lifecycle rules enforced by the producer state. This
     *         exception is thrown on a best effort basis and programs should
     *         not rely on it for correct operation.
     */
    @Nonnull DOMDataWriteTransaction createTransaction(boolean barrier);

    /**
     * {@inheritDoc}
     *
     * When invoked on a {@link DOMDataTreeProducer}, this method has additional restrictions.
     * There may not be an open transaction from this producer. The method needs to be
     * invoked in appropriate context. Specified subtrees must be accessible by this producer.
     *
     * Once this method returns successfully, this (parent) producer loses the ability to
     * access the specified paths until the resulting (child) producer is shut down.
     *
     * @throws {@link IllegalStateException} if there is an open transaction
     * @throws {@link IllegalArgumentException} if subtrees contains a subtree which is not
     *         accessible by this producer
     * @throws {@link IllegalThreadStateException} if the calling thread context does not
     *         match the lifecycle rules enforced by the producer state. This
     *         exception is thrown on a best effort basis and programs should
     *         not rely on it for correct operation.
     */
    @Override
    @Nonnull DOMDataTreeProducer createProducer(@Nonnull Collection<DOMDataTreeIdentifier> subtrees);

    /**
     * {@inheritDoc}
     *
     * @throws DOMDataTreeProducerBusyException when there is an open transaction.
     */
    @Override
    void close() throws DOMDataTreeProducerException;
}
