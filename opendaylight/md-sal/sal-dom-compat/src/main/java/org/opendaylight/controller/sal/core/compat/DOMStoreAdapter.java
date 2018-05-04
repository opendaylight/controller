/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import com.google.common.collect.ForwardingObject;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTreeChangePublisher;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public abstract class DOMStoreAdapter<T extends org.opendaylight.mdsal.dom.spi.store.DOMStore
        & org.opendaylight.mdsal.dom.spi.store.DOMStoreTreeChangePublisher> extends ForwardingObject
        implements DOMStore, DOMStoreTreeChangePublisher {
    @Override
    protected abstract T delegate();

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new DOMStoreReadTransactionAdapter<>(delegate().newReadOnlyTransaction());
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return new DOMStoreReadWriteTransactionAdapter(delegate().newReadWriteTransaction());
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new DOMStoreWriteTransactionAdapter(delegate().newWriteOnlyTransaction());
    }

    @Override
    public DOMStoreTransactionChain createTransactionChain() {
        return new DOMStoreTransactionChainAdapter(delegate().createTransactionChain());
    }

    @Override
    public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerTreeChangeListener(
            final YangInstanceIdentifier treeId, final L listener) {
        final ListenerRegistration<?> reg = delegate().registerTreeChangeListener(treeId, listener::onDataTreeChanged);
        return new AbstractListenerRegistration<L>(listener) {
            @Override
            protected void removeRegistration() {
                reg.close();
            }
        };
    }
}
