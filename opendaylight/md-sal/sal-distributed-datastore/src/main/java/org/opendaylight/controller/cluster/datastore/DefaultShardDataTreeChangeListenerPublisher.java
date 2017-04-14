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
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.spi.AbstractDOMDataTreeChangeListenerRegistration;
import org.opendaylight.mdsal.dom.spi.store.AbstractDOMStoreTreeChangePublisher;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of ShardDataTreeChangeListenerPublisher that directly generates and publishes
 * notifications for DataTreeChangeListeners.
 *
 * @author Thomas Pantelis
 */
@NotThreadSafe
final class DefaultShardDataTreeChangeListenerPublisher extends AbstractDOMStoreTreeChangePublisher
        implements ShardDataTreeChangeListenerPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultShardDataTreeChangeListenerPublisher.class);

    @Override
    public void publishChanges(final DataTreeCandidate candidate, String logContext) {
        processCandidateTree(candidate);
    }

    @Override
    protected void notifyListener(AbstractDOMDataTreeChangeListenerRegistration<?> registration,
            Collection<DataTreeCandidate> changes) {
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
        AbstractDOMDataTreeChangeListenerRegistration<org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener>
            registration = super.registerTreeChangeListener(treeId,
                (org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener)changes ->
                    listener.onDataTreeChanged(changes));

        onRegistration.accept(
            new org.opendaylight.controller.md.sal.dom.spi.AbstractDOMDataTreeChangeListenerRegistration<
                    DOMDataTreeChangeListener>(listener) {
                @Override
                protected void removeRegistration() {
                    registration.close();
                }
            });

        if (initialState.isPresent()) {
            notifySingleListener(treeId, listener, initialState.get());
        }
    }

    static void notifySingleListener(YangInstanceIdentifier treeId, DOMDataTreeChangeListener listener,
            DataTreeCandidate state) {
        DefaultShardDataTreeChangeListenerPublisher publisher = new DefaultShardDataTreeChangeListenerPublisher();
        publisher.registerTreeChangeListener(treeId, listener, Optional.absent(), noop -> { });
        publisher.publishChanges(state, "");
    }
}
