/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Stopwatch;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.dom.spi.AbstractDOMDataTreeChangeListenerRegistration;
import org.opendaylight.controller.sal.core.spi.data.AbstractDOMStoreTreeChangePublisher;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;
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

    private final Stopwatch timer = Stopwatch.createUnstarted();

    @Override
    public void publishChanges(final DataTreeCandidate candidate, String logContext) {
        timer.start();

        try {
            processCandidateTree(candidate);
        } finally {
            timer.stop();
            long elapsedTime = timer.elapsed(TimeUnit.MILLISECONDS);
            if(elapsedTime >= PUBLISH_DELAY_THRESHOLD_IN_MS) {
                LOG.warn("{}: Generation of DataTreeCandidateNode events took longer than expected. Elapsed time: {}",
                        logContext, timer);
            } else {
                LOG.debug("{}: Elapsed time for generation of DataTreeCandidateNode events: {}", logContext, timer);
            }

            timer.reset();
        }
    }

    @Override
    public ShardDataTreeChangeListenerPublisher newInstance() {
        return new DefaultShardDataTreeChangeListenerPublisher();
    }

    @Override
    protected void notifyListeners(final Collection<AbstractDOMDataTreeChangeListenerRegistration<?>> registrations,
            final YangInstanceIdentifier path, final DataTreeCandidateNode node) {
        final Collection<DataTreeCandidate> changes = Collections.<DataTreeCandidate>singleton(
                DataTreeCandidates.newDataTreeCandidate(path, node));

        for (AbstractDOMDataTreeChangeListenerRegistration<?> reg : registrations) {
            reg.getInstance().onDataTreeChanged(changes);
        }
    }

    @Override
    protected void registrationRemoved(final AbstractDOMDataTreeChangeListenerRegistration<?> registration) {
        LOG.debug("Registration {} removed", registration);
    }
}
