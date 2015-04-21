/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.api;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainFactory;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Provides access to a conceptual data tree store and also provides the ability to
 * subscribe for changes to data under a given branch of the tree.
 * <p>
 * For more information on usage, please see the documentation in {@link AsyncDataBroker}.
 *
 * @see AsyncDataBroker
 * @see TransactionChainFactory
 */
public interface DataBroker extends  AsyncDataBroker<InstanceIdentifier<?>, DataObject, DataChangeListener>,
    TransactionChainFactory<InstanceIdentifier<?>, DataObject>, TransactionFactory, BindingService, DataTreeChangeService {
    /**
     * {@inheritDoc}
     */
    @Override
    ReadOnlyTransaction newReadOnlyTransaction();

    /**
     * {@inheritDoc}
     */
    @Override
    ReadWriteTransaction newReadWriteTransaction();

    /**
     * {@inheritDoc}
     */
    @Override
    WriteTransaction newWriteOnlyTransaction();

    /**
     * {@inheritDoc}
     */
    @Override
    ListenerRegistration<DataChangeListener> registerDataChangeListener(LogicalDatastoreType store,
            InstanceIdentifier<?> path, DataChangeListener listener, DataChangeScope triggeringScope);

    /**
     * {@inheritDoc}
     */
    @Override
    BindingTransactionChain createTransactionChain(TransactionChainListener listener);
}
