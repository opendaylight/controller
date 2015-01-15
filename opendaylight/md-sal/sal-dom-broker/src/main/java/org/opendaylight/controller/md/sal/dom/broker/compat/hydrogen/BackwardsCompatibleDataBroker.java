/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package org.opendaylight.controller.md.sal.dom.broker.compat.hydrogen;

import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.md.sal.common.api.RegistrationListener;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandlerRegistration;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.controller.sal.core.api.data.DataChangeListener;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.data.DataValidator;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

@Deprecated
@ThreadSafe
public class BackwardsCompatibleDataBroker implements DataProviderService {

    private final DOMDataBroker backingBroker;
    private volatile DataNormalizer normalizer;
    private final ListenerRegistration<SchemaContextListener> schemaReg;

    public BackwardsCompatibleDataBroker(final DOMDataBroker newBiDataImpl, final SchemaService schemaService) {
        backingBroker = newBiDataImpl;
        schemaReg = schemaService.registerSchemaContextListener(new SchemaListener());
    }

    @Override
    public CompositeNode readConfigurationData(final YangInstanceIdentifier legacyPath) {
        final BackwardsCompatibleTransaction<?> tx = BackwardsCompatibleTransaction.readOnlyTransaction(backingBroker.newReadOnlyTransaction(),normalizer);
        try {
            return tx.readConfigurationData(legacyPath);
        } finally {
            tx.commit();
        }
    }

    @Override
    public CompositeNode readOperationalData(final YangInstanceIdentifier legacyPath) {
        final BackwardsCompatibleTransaction<?> tx = BackwardsCompatibleTransaction.readOnlyTransaction(backingBroker.newReadOnlyTransaction(),normalizer);
        try {
            return tx.readOperationalData(legacyPath);
        } finally {
            tx.commit();
        }
    }

    @Override
    public DataModificationTransaction beginTransaction() {
        return BackwardsCompatibleTransaction.readWriteTransaction(backingBroker.newReadWriteTransaction(), normalizer);
    }

    @Override
    public ListenerRegistration<DataChangeListener> registerDataChangeListener(final YangInstanceIdentifier legacyPath,
            final DataChangeListener listener) {
        final YangInstanceIdentifier normalizedPath = normalizer.toNormalized(legacyPath);

        final TranslatingListenerInvoker translatingCfgListener =
                TranslatingListenerInvoker.createConfig(listener, normalizer);
        translatingCfgListener.register(backingBroker, normalizedPath);

        final TranslatingListenerInvoker translatingOpListener =
                TranslatingListenerInvoker.createOperational(listener, normalizer);
        translatingOpListener.register(backingBroker, normalizedPath);

        return new DelegateListenerRegistration(translatingCfgListener, translatingOpListener, listener);
    }

    @Override
    public Registration registerCommitHandler(
            final YangInstanceIdentifier path, final DataCommitHandler<YangInstanceIdentifier, CompositeNode> commitHandler) {
        // FIXME Do real forwarding
        return new AbstractObjectRegistration<DataCommitHandler<YangInstanceIdentifier,CompositeNode>>(commitHandler) {
            @Override
            protected void removeRegistration() {
                // NOOP
            }
        };
    }

    @Override
    public ListenerRegistration<RegistrationListener<DataCommitHandlerRegistration<YangInstanceIdentifier, CompositeNode>>> registerCommitHandlerListener(
            final RegistrationListener<DataCommitHandlerRegistration<YangInstanceIdentifier, CompositeNode>> commitHandlerListener) {
        return null;
    }

    // Obsolete functionality

    @Override
    public void addValidator(final DataStoreIdentifier store, final DataValidator validator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeValidator(final DataStoreIdentifier store, final DataValidator validator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addRefresher(final DataStoreIdentifier store, final DataRefresher refresher) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeRefresher(final DataStoreIdentifier store, final DataRefresher refresher) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Registration registerConfigurationReader(
            final YangInstanceIdentifier path, final DataReader<YangInstanceIdentifier, CompositeNode> reader) {
        throw new UnsupportedOperationException("Data Reader contract is not supported.");
    }

    @Override
    public Registration registerOperationalReader(
            final YangInstanceIdentifier path, final DataReader<YangInstanceIdentifier, CompositeNode> reader) {
        throw new UnsupportedOperationException("Data Reader contract is not supported.");
    }

    private static class DelegateListenerRegistration implements ListenerRegistration<DataChangeListener> {
        private final TranslatingListenerInvoker translatingCfgListener;
        private final TranslatingListenerInvoker translatingOpListener;
        private final DataChangeListener listener;

        public DelegateListenerRegistration(final TranslatingListenerInvoker translatingCfgListener, final TranslatingListenerInvoker translatingOpListener, final DataChangeListener listener) {
            this.translatingCfgListener = translatingCfgListener;
            this.translatingOpListener = translatingOpListener;
            this.listener = listener;
        }

        @Override
        public void close() {
            translatingCfgListener.close();
            translatingOpListener.close();
        }

        @Override
        public DataChangeListener getInstance() {
            return listener;
        }
    }

    private class SchemaListener implements SchemaContextListener {

        @Override
        public void onGlobalContextUpdated(final SchemaContext ctx) {
            normalizer = new DataNormalizer(ctx);
        }

    }
}
