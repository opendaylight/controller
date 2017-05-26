/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.databroker;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeCommitCohortRegistry;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTreeChangePublisher;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractDOMBroker extends AbstractDOMTransactionFactory<DOMStore>
        implements DOMDataBroker {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDOMBroker.class);

    private final AtomicLong txNum = new AtomicLong();
    private final AtomicLong chainNum = new AtomicLong();
    private final Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> extensions;
    private volatile AutoCloseable closeable;

    protected AbstractDOMBroker(final Map<LogicalDatastoreType, DOMStore> datastores) {
        super(datastores);

        Builder<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> extBuilder = ImmutableMap.builder();
        if (isSupported(datastores, DOMStoreTreeChangePublisher.class)) {
            extBuilder.put(DOMDataTreeChangeService.class, new DOMDataTreeChangeService() {
                @Override
                public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerDataTreeChangeListener(
                        final DOMDataTreeIdentifier treeId, final L listener) {
                    DOMStore store = getTxFactories().get(treeId.getDatastoreType());
                    checkState(store != null, "Requested logical data store is not available.");

                    return ((DOMStoreTreeChangePublisher) store).registerTreeChangeListener(
                            treeId.getRootIdentifier(), listener);
                }
            });
        }

        if (isSupported(datastores, org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry.class)) {
            extBuilder.put(DOMDataTreeCommitCohortRegistry.class, new DOMDataTreeCommitCohortRegistry() {
                @Override
                public <T extends DOMDataTreeCommitCohort> DOMDataTreeCommitCohortRegistration<T> registerCommitCohort(
                        org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier path, T cohort) {
                    DOMStore store = getTxFactories().get(toLegacy(path.getDatastoreType()));
                    checkState(store != null, "Requested logical data store is not available.");

                    return ((org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry) store)
                            .registerCommitCohort(path, cohort);
                }
            });
        }

        extensions = extBuilder.build();
    }

    private static LogicalDatastoreType toLegacy(org.opendaylight.mdsal.common.api.LogicalDatastoreType datastoreType) {
        switch (datastoreType) {
            case CONFIGURATION:
                return LogicalDatastoreType.CONFIGURATION;
            case OPERATIONAL:
                return LogicalDatastoreType.OPERATIONAL;
            default:
                throw new IllegalArgumentException("Unsupported data store type: " + datastoreType);
        }
    }

    private static boolean isSupported(Map<LogicalDatastoreType, DOMStore> datastores,
            Class<?> expDOMStoreInterface) {
        for (DOMStore ds : datastores.values()) {
            if (!expDOMStoreInterface.isAssignableFrom(ds.getClass())) {
                return false;
            }
        }

        return true;
    }

    public void setCloseable(final AutoCloseable closeable) {
        this.closeable = closeable;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void close() {
        super.close();

        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                LOG.debug("Error closing instance", e);
            }
        }
    }

    @Override
    protected Object newTransactionIdentifier() {
        return "DOM-" + txNum.getAndIncrement();
    }

    @Override
    public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(final LogicalDatastoreType store,
            final YangInstanceIdentifier path, final DOMDataChangeListener listener,
            final DataChangeScope triggeringScope) {
        DOMStore potentialStore = getTxFactories().get(store);
        checkState(potentialStore != null, "Requested logical data store is not available.");
        return potentialStore.registerChangeListener(path, listener, triggeringScope);
    }

    @Override
    public Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> getSupportedExtensions() {
        return extensions;
    }

    @Override
    public DOMTransactionChain createTransactionChain(final TransactionChainListener listener) {
        checkNotClosed();

        final Map<LogicalDatastoreType, DOMStoreTransactionChain> backingChains =
                new EnumMap<>(LogicalDatastoreType.class);
        for (Map.Entry<LogicalDatastoreType, DOMStore> entry : getTxFactories().entrySet()) {
            backingChains.put(entry.getKey(), entry.getValue().createTransactionChain());
        }

        final long chainId = chainNum.getAndIncrement();
        LOG.debug("Transaction chain {} created with listener {}, backing store chains {}", chainId, listener,
                backingChains);
        return new DOMBrokerTransactionChain(chainId, backingChains, this, listener);
    }
}
