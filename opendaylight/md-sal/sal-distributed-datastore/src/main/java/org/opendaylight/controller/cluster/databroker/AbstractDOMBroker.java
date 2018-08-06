/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.databroker;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap.Builder;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistration;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTreeChangePublisher;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDOMBroker extends AbstractDOMTransactionFactory<DOMStore> implements DOMDataBroker {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDOMBroker.class);

    private final AtomicLong txNum = new AtomicLong();
    private final AtomicLong chainNum = new AtomicLong();
    private final ClassToInstanceMap<DOMDataBrokerExtension> extensions;

    private volatile AutoCloseable closeable;

    protected AbstractDOMBroker(final Map<LogicalDatastoreType, DOMStore> datastores) {
        super(datastores);

        Builder<DOMDataBrokerExtension> extBuilder = ImmutableClassToInstanceMap.builder();
        if (isSupported(datastores, DOMStoreTreeChangePublisher.class)) {
            extBuilder.put(DOMDataTreeChangeService.class, new DOMDataTreeChangeService() {
                @Override
                public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerDataTreeChangeListener(
                        final DOMDataTreeIdentifier treeId, final L listener) {
                    DOMStore store = getDOMStore(treeId.getDatastoreType());
                    return ((DOMStoreTreeChangePublisher) store).registerTreeChangeListener(
                            treeId.getRootIdentifier(), listener);
                }
            });
        }

        if (isSupported(datastores, DOMDataTreeCommitCohortRegistry.class)) {
            extBuilder.put(DOMDataTreeCommitCohortRegistry.class, new DOMDataTreeCommitCohortRegistry() {
                @Override
                public <T extends DOMDataTreeCommitCohort> DOMDataTreeCommitCohortRegistration<T> registerCommitCohort(
                        final DOMDataTreeIdentifier path, final T cohort) {
                    DOMStore store = getDOMStore(path.getDatastoreType());
                    return ((DOMDataTreeCommitCohortRegistry) store).registerCommitCohort(path, cohort);
                }
            });
        }

        extensions = extBuilder.build();
    }

    private static boolean isSupported(final Map<LogicalDatastoreType, DOMStore> datastores,
            final Class<?> expDOMStoreInterface) {
        return datastores.values().stream().allMatch(expDOMStoreInterface::isInstance);
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
    public ClassToInstanceMap<DOMDataBrokerExtension> getExtensions() {
        return extensions;
    }

    @Override
    public DOMTransactionChain createTransactionChain(final DOMTransactionChainListener listener) {
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

    private DOMStore getDOMStore(final LogicalDatastoreType type) {
        DOMStore store = getTxFactories().get(type);
        checkState(store != null, "Requested logical data store is not available.");
        return store;
    }
}
