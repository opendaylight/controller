/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.config.distributed_datastore_provider;

import com.google.common.collect.ForwardingObject;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * DOMStore implementation that forwards to a delegate.
 *
 * @author Thomas Pantelis
 */
class ForwardingDistributedDataStore extends ForwardingObject implements DistributedDataStoreInterface, AutoCloseable {
    private final DistributedDataStoreInterface delegate;
    private final AutoCloseable closeable;

    ForwardingDistributedDataStore(DistributedDataStoreInterface delegate, AutoCloseable closeable) {
        this.delegate = delegate;
        this.closeable = closeable;
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return delegate().newReadOnlyTransaction();
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return delegate().newWriteOnlyTransaction();
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return delegate().newReadWriteTransaction();
    }

    @Override
    public <L extends AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> ListenerRegistration<L>
            registerChangeListener(YangInstanceIdentifier path, L listener, DataChangeScope scope) {
        return delegate().registerChangeListener(path, listener, scope);
    }

    @Override
    public DOMStoreTransactionChain createTransactionChain() {
        return delegate().createTransactionChain();
    }

    @Override
    public ActorContext getActorContext() {
        return delegate().getActorContext();
    }

    @Override
    public void close() throws Exception {
        closeable.close();
    }

    @Override
    protected DistributedDataStoreInterface delegate() {
        return delegate;
    }
}
