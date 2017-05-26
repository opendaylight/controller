/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.util.Collection;
import javax.annotation.concurrent.NotThreadSafe;
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
    public ShardDataTreeChangeListenerPublisher newInstance() {
        return new DefaultShardDataTreeChangeListenerPublisher();
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
    public <L extends org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener> ListenerRegistration<L>
            registerTreeChangeListener(final YangInstanceIdentifier treeId, final L listener) {
        final AbstractDOMDataTreeChangeListenerRegistration<DOMDataTreeChangeListener> registration =
            super.registerTreeChangeListener(treeId, (org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener)
                changes -> listener.onDataTreeChanged(changes));

        return new org.opendaylight.controller.md.sal.dom.spi.AbstractDOMDataTreeChangeListenerRegistration<L>(
                listener) {
            @Override
            protected void removeRegistration() {
                registration.close();
            }
        };
    }
}
