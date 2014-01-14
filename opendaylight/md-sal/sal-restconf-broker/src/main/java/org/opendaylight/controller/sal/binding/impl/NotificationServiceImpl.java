/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Notification;

public class NotificationServiceImpl implements NotificationService {
    @Override
    public <T extends Notification> void addNotificationListener(Class<T> notificationType, NotificationListener<T> listener) {

    }

    @Override
    public void addNotificationListener(org.opendaylight.yangtools.yang.binding.NotificationListener listener) {

    }

    @Override
    public void removeNotificationListener(org.opendaylight.yangtools.yang.binding.NotificationListener listener) {

    }

    @Override
    public <T extends Notification> void removeNotificationListener(Class<T> notificationType, NotificationListener<T> listener) {

    }

    @Override
    public <T extends Notification> Registration<NotificationListener<T>> registerNotificationListener(Class<T> notificationType, NotificationListener<T> listener) {
        //TODO implementation using sal-remote
        return null;
    }

    @Override
    public Registration<org.opendaylight.yangtools.yang.binding.NotificationListener> registerNotificationListener(org.opendaylight.yangtools.yang.binding.NotificationListener listener) {
        //TODO implementation using sal-remote
        return null;
    }
}
