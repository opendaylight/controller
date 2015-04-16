/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.NotificationManager;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

final class ShardDataTreeNotificationManager implements NotificationManager<DataChangeListenerRegistration<?>, DOMImmutableDataChangeEvent> {
    @Override
    public void submitNotification(final DataChangeListenerRegistration<?> listener, final DOMImmutableDataChangeEvent notification) {
        listener.getInstance().onDataChanged(notification);
    }

    @Override
    public void submitNotifications(final DataChangeListenerRegistration<?> listener, final Iterable<DOMImmutableDataChangeEvent> notifications) {
        final AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> instance = listener.getInstance();
        for (DOMImmutableDataChangeEvent n : notifications) {
            instance.onDataChanged(n);
        }
    }
}
