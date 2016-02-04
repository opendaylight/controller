/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration;
import org.opendaylight.controller.md.sal.dom.store.impl.ResolveDataChangeEventsTask;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerTree;
import org.opendaylight.yangtools.util.concurrent.NotificationManager;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of ShardDataChangeListenerPublisher that directly generates and publishes
 * notifications for DataChangeListeners.
 *
 * @author Thomas Pantelis
 */
@NotThreadSafe
final class DefaultShardDataChangeListenerPublisher implements ShardDataChangeListenerPublisher,
        NotificationManager<DataChangeListenerRegistration<?>, DOMImmutableDataChangeEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultShardDataChangeListenerPublisher.class);

    private final ListenerTree dataChangeListenerTree = ListenerTree.create();
    private final Stopwatch timer = Stopwatch.createUnstarted();

    @Override
    public void submitNotification(final DataChangeListenerRegistration<?> listener, final DOMImmutableDataChangeEvent notification) {
        LOG.debug("Notifying listener {} about {}", listener.getInstance(), notification);

        listener.getInstance().onDataChanged(notification);
    }

    @Override
    public void submitNotifications(final DataChangeListenerRegistration<?> listener, final Iterable<DOMImmutableDataChangeEvent> notifications) {
        final AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> instance = listener.getInstance();
        LOG.debug("Notifying listener {} about {}", instance, notifications);

        for (DOMImmutableDataChangeEvent n : notifications) {
            instance.onDataChanged(n);
        }
    }

    @Override
    public void publishChanges(DataTreeCandidate candidate, String logContext) {
        timer.start();

        try {
            ResolveDataChangeEventsTask.create(candidate, dataChangeListenerTree).resolve(this);
        } finally {
            timer.stop();
            long elapsedTime = timer.elapsed(TimeUnit.MILLISECONDS);
            if(elapsedTime >= PUBLISH_DELAY_THRESHOLD_IN_MS) {
                LOG.warn("{}: Generation of DataChange events took longer than expected. Elapsed time: {}",
                        logContext, timer);
            } else {
                LOG.debug("{}: Elapsed time for generation of DataChange events: {}", logContext, timer);
            }

            timer.reset();
        }
    }

    @Override
    public <L extends AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> DataChangeListenerRegistration<L>
            registerDataChangeListener(YangInstanceIdentifier path, L listener, DataChangeScope scope) {
        return dataChangeListenerTree.registerDataChangeListener(path, listener, scope);
    }

    @Override
    public ShardDataChangeListenerPublisher newInstance() {
        return new DefaultShardDataChangeListenerPublisher();
    }
}
