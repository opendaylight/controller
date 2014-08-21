/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import com.google.common.base.Preconditions;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.util.concurrent.NotificationManager;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ChangeListenerNotifyTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ChangeListenerNotifyTask.class);

    @SuppressWarnings("rawtypes")
    private final NotificationManager<AsyncDataChangeListener,AsyncDataChangeEvent> notificationMgr;
    private final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> event;
    private final DataChangeListenerRegistration<?> listener;

    @SuppressWarnings("rawtypes")
    public ChangeListenerNotifyTask(final DataChangeListenerRegistration<?> listener,
            final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> event,
            final NotificationManager<AsyncDataChangeListener,AsyncDataChangeEvent> notificationMgr) {
        this.notificationMgr = Preconditions.checkNotNull(notificationMgr);
        this.listener = Preconditions.checkNotNull(listener);
        this.event = Preconditions.checkNotNull(event);
    }

    @Override
    public void run() {
        final AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> l = listener.getInstance();
        if (l == null) {
            LOG.trace("Skipping event delivery to unregistered listener {}", l);
            return;
        }
        LOG.trace("Listener {} event {}", l, event);

        // FIXME: Yo dawg I heard you like queues, so this was queued to be queued
        notificationMgr.submitNotification(l, event);
    }

    @Override
    public String toString() {
        return "ChangeListenerNotifyTask [listener=" + listener + ", event=" + event + "]";
    }
}
