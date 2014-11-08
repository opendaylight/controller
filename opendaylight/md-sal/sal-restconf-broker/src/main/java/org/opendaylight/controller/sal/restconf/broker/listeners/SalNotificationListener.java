/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.broker.listeners;

import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.yangtools.yang.binding.Notification;


public class SalNotificationListener<T extends Notification> implements NotificationListener<T> {
    private NotificationListener<T> notificationListener;

    public SalNotificationListener( NotificationListener<T> notificationListener){
        this.notificationListener = notificationListener;
    }
    @Override
    public void onNotification(Notification notification) {
        this.notificationListener.onNotification((T)notification);
    }
}
