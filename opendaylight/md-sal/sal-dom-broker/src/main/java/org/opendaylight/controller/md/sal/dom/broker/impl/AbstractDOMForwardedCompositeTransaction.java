package org.opendaylight.controller.md.sal.dom.broker.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * Composite DOM Transaction backed by {@link DOMStoreTransaction}.
 *
 * Abstract base for composite transaction, which provides access only
 * to common functionality as retrieval of subtransaction,
 * close method and retrieval of identifier.
 *
 * @param <K> Subtransaction distinguisher
 * @param <T> Subtransaction type
 */
abstract class AbstractDOMForwardedCompositeTransaction<K, T extends DOMStoreTransaction> implements
        AsyncTransaction<InstanceIdentifier, NormalizedNode<?, ?>> {

    private final ImmutableMap<K, T> backingTxs;
    private final Object identifier;

    /**
     *
     * Creates new composite Transactions.
     *
     * @param identifier Identifier of transaction.
     * @param backingTxs Key,value map of backing transactions.
     */
    protected AbstractDOMForwardedCompositeTransaction(final Object identifier, final ImmutableMap<K, T> backingTxs) {
        this.identifier = checkNotNull(identifier, "Identifier should not be null");
        this.backingTxs = checkNotNull(backingTxs, "Backing transactions should not be null");
    }

    /**
     * Returns subtransaction associated with supplied key.
     *
     * @param key
     * @return
     * @throws NullPointerException if key is null
     * @throws IllegalArgumentException if no subtransaction is associated with key.
     */
    protected final T getSubtransaction(final K key) {
        Preconditions.checkNotNull(key,"key must not be null.");
        Preconditions.checkArgument(backingTxs.containsKey(key),"No subtransaction associated with %s",key);
        return backingTxs.get(key);
    }

    /**
     * Returns immutable Iterable of all subtransactions.
     *
     */
    protected Iterable<T> getSubtransactions() {
        return backingTxs.values();
    }

    @Override
    public Object getIdentifier() {
        return identifier;
    }

    @Override
    public void close() {
        try {
            for (T subtransaction : backingTxs.values()) {
                subtransaction.close();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Uncaught exception occured during closing transaction.", e);
        }
    }
}