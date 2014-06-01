/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.base.Preconditions;

/**
 * A listener registration type. Objects of this type encapsulate a single
 * registration and provide the means to do atomic notification closure.
 *
 * @param <T> Notification type
 */
abstract class NotificationListenerRegistration<T extends Notification> extends AbstractListenerRegistration<NotificationListener<T>> {
    private final Class<?> type;

    protected NotificationListenerRegistration(final Class<?> type, final NotificationListener<T> listener) {
        super(listener);
        this.type = Preconditions.checkNotNull(type);
    }

    public Class<?> getType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    public void notify(final Notification notification) {
        if (!isClosed()) {
            getInstance().onNotification((T)notification);
        }
    }
}
