/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Map;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Composite DOM Transaction backed by {@link DOMStoreTransaction}.
 *
 * Abstract base for composite transaction, which provides access only to common
 * functionality as retrieval of subtransaction, close method and retrieval of
 * identifier.
 *
 * @param <K>
 *            Subtransaction distinguisher
 * @param <T>
 *            Subtransaction type
 */
abstract class AbstractDOMForwardedCompositeTransaction<K, T extends DOMStoreTransaction> implements
        AsyncTransaction<YangInstanceIdentifier, NormalizedNode<?, ?>> {

    private final Map<K, T> backingTxs;
    private final Object identifier;

    /**
     *
     * Creates new composite Transactions.
     *
     * @param identifier
     *            Identifier of transaction.
     * @param backingTxs
     *            Key,value map of backing transactions.
     */
    protected AbstractDOMForwardedCompositeTransaction(final Object identifier, final Map<K, T> backingTxs) {
        this.identifier = Preconditions.checkNotNull(identifier, "Identifier should not be null");
        this.backingTxs = Preconditions.checkNotNull(backingTxs, "Backing transactions should not be null");
    }

    /**
     * Returns subtransaction associated with supplied key.
     *
     * @param key
     * @return
     * @throws NullPointerException
     *             if key is null
     * @throws IllegalArgumentException
     *             if no subtransaction is associated with key.
     */
    protected final T getSubtransaction(final K key) {
        Preconditions.checkNotNull(key, "key must not be null.");

        final T ret = backingTxs.get(key);
        Preconditions.checkArgument(ret != null, "No subtransaction associated with %s", key);
        return ret;
    }

    /**
     * Returns immutable Iterable of all subtransactions.
     *
     */
    protected Collection<T> getSubtransactions() {
        return backingTxs.values();
    }

    @Override
    public Object getIdentifier() {
        return identifier;
    }

    protected void closeSubtransactions() {
        /*
         * We share one exception for all failures, which are added
         * as supressedExceptions to it.
         */
        IllegalStateException failure = null;
        for (T subtransaction : backingTxs.values()) {
            try {
                subtransaction.close();
            } catch (Exception e) {
                // If we did not allocated failure we allocate it
                if (failure == null) {
                    failure = new IllegalStateException("Uncaught exception occured during closing transaction", e);
                } else {
                    // We update it with additional exceptions, which occurred during error.
                    failure.addSuppressed(e);
                }
            }
        }
        // If we have failure, we throw it at after all attempts to close.
        if (failure != null) {
            throw failure;
        }
    }
}
