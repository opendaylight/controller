/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi.data;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * DOM Data Store
 *
 * <p>
 * DOM Data Store provides transactional tree-like storage for YANG-modeled
 * entities described by YANG schema and represented by {@link NormalizedNode}.
 *
 * Read and write access to stored data is provided only via transactions
 * created using {@link #newReadOnlyTransaction()},
 * {@link #newWriteOnlyTransaction()} and {@link #newReadWriteTransaction()}, or
 * by creating {@link org.opendaylight.controller.md.sal.common.api.data.TransactionChain}.
 *
 */
public interface DOMStore extends DOMStoreTransactionFactory {

    /**
     * Registers {@link org.opendaylight.controller.md.sal.common.api.data.DataChangeListener} for Data Change callbacks which will
     * be triggered on the change of provided subpath. What constitutes a change
     * depends on the @scope parameter.
     *
     * Listener upon registration receives an initial callback
     * {@link AsyncDataChangeListener#onDataChanged(org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent)}
     * which contains stable view of data tree at the time of registration.
     *
     *  @param path Path (subtree identifier) on which client listener will be
     * invoked.
     *
     * @param listener
     *            Instance of listener which should be invoked on
     * @param scope
     *            Scope of change which triggers callback.
     * @return Listener Registration object, which client may use to close
     *         registration / interest on receiving data changes.
     *
     */
    <L extends AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> ListenerRegistration<L> registerChangeListener(
            YangInstanceIdentifier path, L listener, DataChangeScope scope);

    /**
     *
     * Creates new transaction chain.
     *
     * Transactions in a chain need to be committed in sequence and each
     * transaction should see the effects of previous transactions as if they
     * happened.
     *
     * See {@link DOMStoreTransactionChain} for more information.
     *
     * @return Newly created transaction chain.
     */
    DOMStoreTransactionChain createTransactionChain();

}
