/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.api;

import com.google.common.collect.ForwardingObject;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Utility {@link DataBroker} implementation which forwards all interface method
 * invocation to a delegate instance.
 */
public abstract class ForwardingDataBroker extends ForwardingObject implements DataBroker {

    @Override
    protected abstract @Nonnull DataBroker delegate();

    @Override
    public ReadOnlyTransaction newReadOnlyTransaction() {
        return delegate().newReadOnlyTransaction();
    }

    @Override
    public ReadWriteTransaction newReadWriteTransaction() {
        return delegate().newReadWriteTransaction();
    }

    @Override
    public WriteTransaction newWriteOnlyTransaction() {
        return delegate().newWriteOnlyTransaction();
    }

    @Override
    public <T extends DataObject, L extends DataTreeChangeListener<T>> ListenerRegistration<L>
            registerDataTreeChangeListener(DataTreeIdentifier<T> treeId, L listener) {
        return delegate().registerDataTreeChangeListener(treeId, listener);
    }

    @Override
    public ListenerRegistration<DataChangeListener> registerDataChangeListener(LogicalDatastoreType store,
            InstanceIdentifier<?> path, DataChangeListener listener, DataChangeScope triggeringScope) {
        return delegate().registerDataChangeListener(store, path, listener, triggeringScope);
    }

    @Override
    public BindingTransactionChain createTransactionChain(TransactionChainListener listener) {
        return delegate().createTransactionChain(listener);
    }

}
