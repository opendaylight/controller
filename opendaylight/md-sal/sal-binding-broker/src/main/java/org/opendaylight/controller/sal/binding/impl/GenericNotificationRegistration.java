/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Notification;

public class GenericNotificationRegistration<T extends Notification> extends AbstractObjectRegistration<NotificationListener<T>> implements ListenerRegistration<NotificationListener<T>> {
    private final Class<T> _type;

    public Class<T> getType() {
        return this._type;
    }

    private NotificationBrokerImpl notificationBroker;

    public GenericNotificationRegistration(final Class<T> type, final NotificationListener<T> instance, final NotificationBrokerImpl broker) {
        super(instance);
        this._type = type;
        this.notificationBroker = broker;
    }

    @Override
    protected void removeRegistration() {
        this.notificationBroker.unregisterListener(this);
        this.notificationBroker = null;
    }
}
