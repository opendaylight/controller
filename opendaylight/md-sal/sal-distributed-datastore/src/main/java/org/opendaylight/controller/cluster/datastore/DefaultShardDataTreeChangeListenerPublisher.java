/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.spi.store.AbstractDOMStoreTreeChangePublisher;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
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

    private String logName;

    DefaultShardDataTreeChangeListenerPublisher(final String logName) {
        this.logName = logName;
    }

    @Override
    public void publishChanges(final DataTreeCandidate candidate) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("{}: publishChanges: {}", logName, candidate);
        } else {
            LOG.debug("{}: publishChanges: rootPath: {}", logName, candidate.getRootPath());
        }

        processCandidateTree(candidate);
    }

    @Override
    protected void notifyListener(final Reg registration, final List<DataTreeCandidate> changes) {
        if (registration.notClosed()) {
            final var listener = registration.listener();
            LOG.debug("{}: notifyListener: listener: {}", logName, listener);
            listener.onDataTreeChanged(changes);
        }
    }

    @Override
    protected void registrationRemoved(final Reg registration) {
        LOG.debug("Registration {} removed", registration);
    }

    @Override
    public void registerTreeChangeListener(final YangInstanceIdentifier treeId,
            final DOMDataTreeChangeListener listener, final Optional<DataTreeCandidate> initialState,
            final Consumer<Registration> onRegistration) {
        registerTreeChangeListener(treeId, listener, onRegistration);

        if (initialState.isPresent()) {
            notifySingleListener(treeId, listener, initialState.orElseThrow(), logName);
        } else {
            listener.onInitialData();
        }
    }

    void registerTreeChangeListener(final YangInstanceIdentifier treeId, final DOMDataTreeChangeListener listener,
            final Consumer<Registration> onRegistration) {
        LOG.debug("{}: registerTreeChangeListener: path: {}, listener: {}", logName, treeId, listener);
        onRegistration.accept(super.registerTreeChangeListener(treeId, listener));
    }

    static void notifySingleListener(final YangInstanceIdentifier treeId, final DOMDataTreeChangeListener listener,
            final DataTreeCandidate state, final String logContext) {
        LOG.debug("{}: notifySingleListener: path: {}, listener: {}", logContext, treeId, listener);
        final var publisher = new DefaultShardDataTreeChangeListenerPublisher(logContext);
        publisher.logName = logContext;
        publisher.registerTreeChangeListener(treeId, listener);

        if (!publisher.processCandidateTree(state)) {
            listener.onInitialData();
        }
    }
}
