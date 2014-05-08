/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ChangeListenerNotifyTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ChangeListenerNotifyTask.class);
    private final Iterable<? extends DataChangeListenerRegistration<?>> listeners;
    private final AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> event;

    public ChangeListenerNotifyTask(final Iterable<? extends DataChangeListenerRegistration<?>> listeners,
            final AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> event) {
        this.listeners = listeners;
        this.event = event;
    }

    @Override
    public void run() {

        for (DataChangeListenerRegistration<?> listener : listeners) {
            try {
                listener.getInstance().onDataChanged(event);
            } catch (Exception e) {
                LOG.error("Unhandled exception during invoking listener {} with event {}", listener, event, e);
            }
        }

    }

    @Override
    public String toString() {
        return "ChangeListenerNotifyTask [listeners=" + listeners + ", event=" + event + "]";
    }

}
