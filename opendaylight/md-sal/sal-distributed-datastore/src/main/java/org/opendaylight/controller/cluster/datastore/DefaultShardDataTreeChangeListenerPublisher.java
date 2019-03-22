/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.function.Consumer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.spi.AbstractDOMDataTreeChangeListenerRegistration;
import org.opendaylight.mdsal.dom.spi.store.AbstractDOMStoreTreeChangePublisher;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of ShardDataTreeChangeListenerPublisher that directly generates and publishes
 * notifications for DataTreeChangeListeners. This class is NOT thread-safe.
 *
 * @author Thomas Pantelis
 */
final class DefaultShardDataTreeChangeListenerPublisher extends AbstractDOMStoreTreeChangePublisher
        implements ShardDataTreeChangeListenerPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultShardDataTreeChangeListenerPublisher.class);
    private String logContext;

    DefaultShardDataTreeChangeListenerPublisher(String logContext) {
        this.logContext = logContext;
    }

    @Override
    public void publishChanges(final DataTreeCandidate candidate) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("{}: publishChanges: {}", logContext, candidate);
        } else {
            LOG.debug("{}: publishChanges: rootPath: {}", logContext, candidate.getRootPath());
        }

        processCandidateTree(candidate);
    }

    @Override
    protected void notifyListener(AbstractDOMDataTreeChangeListenerRegistration<?> registration,
            Collection<DataTreeCandidate> changes) {
        LOG.debug("{}: notifyListener: listener: {}", logContext, registration.getInstance());
        registration.getInstance().onDataTreeChanged(changes);
    }

    @Override
    protected void registrationRemoved(final AbstractDOMDataTreeChangeListenerRegistration<?> registration) {
        LOG.debug("Registration {} removed", registration);
    }

    @Override
    public void registerTreeChangeListener(YangInstanceIdentifier treeId, DOMDataTreeChangeListener listener,
            Optional<DataTreeCandidate> initialState,
            Consumer<ListenerRegistration<DOMDataTreeChangeListener>> onRegistration) {
        registerTreeChangeListener(treeId, listener, onRegistration);

        if (initialState.isPresent()) {
            notifySingleListener(treeId, listener, initialState.get(), logContext);
        } else {
            listener.onInitialData();
        }
    }

    void registerTreeChangeListener(YangInstanceIdentifier treeId, DOMDataTreeChangeListener listener,
            Consumer<ListenerRegistration<DOMDataTreeChangeListener>> onRegistration) {
        LOG.debug("{}: registerTreeChangeListener: path: {}, listener: {}", logContext, treeId, listener);

        AbstractDOMDataTreeChangeListenerRegistration<DOMDataTreeChangeListener> registration =
                super.registerTreeChangeListener(treeId, listener);

        onRegistration.accept(registration);
    }

    static void notifySingleListener(YangInstanceIdentifier treeId, DOMDataTreeChangeListener listener,
            DataTreeCandidate state, String logContext) {
        LOG.debug("{}: notifySingleListener: path: {}, listener: {}", logContext, treeId, listener);
        DefaultShardDataTreeChangeListenerPublisher publisher =
                new DefaultShardDataTreeChangeListenerPublisher(logContext);
        publisher.logContext = logContext;
        publisher.registerTreeChangeListener(treeId, listener);

        if (!publisher.processCandidateTree(state)) {
            listener.onInitialData();
        }
    }
}
