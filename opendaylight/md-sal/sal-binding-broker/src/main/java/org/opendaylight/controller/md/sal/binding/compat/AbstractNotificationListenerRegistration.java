/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.compat;

import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.base.Preconditions;

/**
 * Abstract implementation of {@link NotificationListenerRegistration}.
 *
 * @param <T> Notification type
 */
abstract class AbstractNotificationListenerRegistration<T extends Notification> extends AbstractListenerRegistration<NotificationListener<T>> implements NotificationListenerRegistration<T> {
    private final Class<? extends Notification> type;

    protected AbstractNotificationListenerRegistration(final Class<? extends Notification> type, final NotificationListener<T> listener) {
        super(listener);
        this.type = Preconditions.checkNotNull(type);
    }

    @Override
    public Class<? extends Notification> getType() {
        return type;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void notify(final Notification notification) {
        if (!isClosed()) {
            getInstance().onNotification((T)notification);
        }
    }
}
