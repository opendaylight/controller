/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.compat.hydrogen;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.sal.core.api.data.DataChangeListener;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

@Deprecated
abstract class TranslatingListenerInvoker implements AutoCloseable, DOMDataChangeListener, Delegator<DataChangeListener> {

    private final DataChangeListener delegate;
    private final DataNormalizer normalizer;
    protected ListenerRegistration<DOMDataChangeListener> reg;

    protected TranslatingListenerInvoker(final DataChangeListener listener, final DataNormalizer normalizer) {
        this.delegate = listener;
        this.normalizer = normalizer;
    }

    static TranslatingListenerInvoker createConfig(final DataChangeListener listener, final DataNormalizer normalizer) {
        return new TranslatingConfigListenerInvoker(listener, normalizer);
    }

    static TranslatingListenerInvoker createOperational(final DataChangeListener listener, final DataNormalizer normalizer) {
        return new TranslatingOperationalListenerInvoker(listener, normalizer);
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedChange) {
        delegate.onDataChanged(getLegacyEvent(normalizer, normalizedChange));
    }

    abstract DataChangeEvent<YangInstanceIdentifier, CompositeNode> getLegacyEvent(final DataNormalizer normalizer,
                                                                               final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedChange);

    @Override
    public DataChangeListener getDelegate() {
        return delegate;
    }

    abstract void register(final DOMDataBroker backingBroker, final YangInstanceIdentifier normalizedPath);

    @Override
    public void close() {
        if (reg != null) {
            reg.close();
        }
    }

    @Override
    public String toString() {
        return getDelegate().getClass().getName();
    }

    static final class TranslatingConfigListenerInvoker extends TranslatingListenerInvoker {

        public TranslatingConfigListenerInvoker(final DataChangeListener listener, final DataNormalizer normalizer) {
            super(listener, normalizer);
        }

        @Override
        DataChangeEvent<YangInstanceIdentifier, CompositeNode> getLegacyEvent(final DataNormalizer normalizer, final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedChange) {
            return TranslatingDataChangeEvent.createConfiguration(normalizedChange, normalizer);
        }

        @Override
        void register(final DOMDataBroker backingBroker, final YangInstanceIdentifier normalizedPath) {
            reg = backingBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, normalizedPath, this,
                    AsyncDataBroker.DataChangeScope.SUBTREE);
        }
    }

    static final class TranslatingOperationalListenerInvoker extends TranslatingListenerInvoker {

        public TranslatingOperationalListenerInvoker(final DataChangeListener listener, final DataNormalizer normalizer) {
            super(listener, normalizer);
        }

        @Override
        DataChangeEvent<YangInstanceIdentifier, CompositeNode> getLegacyEvent(final DataNormalizer normalizer, final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedChange) {
            return TranslatingDataChangeEvent.createOperational(normalizedChange, normalizer);
        }

        @Override
        void register(final DOMDataBroker backingBroker, final YangInstanceIdentifier normalizedPath) {
            reg = backingBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, normalizedPath, this,
                    AsyncDataBroker.DataChangeScope.SUBTREE);
        }
    }

}
