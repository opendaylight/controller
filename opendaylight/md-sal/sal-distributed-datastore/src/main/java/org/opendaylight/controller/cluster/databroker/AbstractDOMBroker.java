/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.spi.PingPongMergingDOMDataBroker;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTreeChangePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDOMBroker extends AbstractDOMTransactionFactory<DOMStore>
        implements PingPongMergingDOMDataBroker {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDOMBroker.class);

    private final AtomicLong txNum = new AtomicLong();
    private final AtomicLong chainNum = new AtomicLong();
    private final @NonNull List<Extension> extensions;

    private volatile AutoCloseable closeable;

    protected AbstractDOMBroker(final Map<LogicalDatastoreType, DOMStore> datastores) {
        super(datastores);

        final var extBuilder = ImmutableList.<Extension>builder();
        if (isSupported(datastores, DOMStoreTreeChangePublisher.class)) {
            extBuilder.add((DOMDataTreeChangeService) (treeId, listener) -> {
               final var store = getDOMStore(treeId.datastore());
               return ((DOMStoreTreeChangePublisher) store).registerTreeChangeListener(treeId.path(), listener);
            });
        }

        if (isSupported(datastores, DOMDataTreeCommitCohortRegistry.class)) {
            extBuilder.add((DOMDataTreeCommitCohortRegistry) (path, cohort) -> {
               final var store = getDOMStore(path.datastore());
               return ((DOMDataTreeCommitCohortRegistry) store).registerCommitCohort(path, cohort);
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
    public List<Extension> supportedExtensions() {
        return extensions;
    }

    @Override
    public DOMTransactionChain createTransactionChain() {
        checkNotClosed();

        final var backingChains = new EnumMap<LogicalDatastoreType, DOMStoreTransactionChain>(
            LogicalDatastoreType.class);
        for (var entry : getTxFactories().entrySet()) {
            backingChains.put(entry.getKey(), entry.getValue().createTransactionChain());
        }

        final long chainId = chainNum.getAndIncrement();
        LOG.debug("Transaction chain {} created, backing store chains {}", chainId, backingChains);
        return new DOMBrokerTransactionChain(chainId, backingChains, this);
    }

    private DOMStore getDOMStore(final LogicalDatastoreType type) {
        DOMStore store = getTxFactories().get(type);
        checkState(store != null, "Requested logical data store is not available.");
        return store;
    }
}
