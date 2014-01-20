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


public class SalNotificationListener implements NotificationListener {
    private NotificationListener notificationListener;

    public SalNotificationListener( NotificationListener notificationListener){
        this.notificationListener = notificationListener;
    }
    @Override
    public void onNotification(Notification notification) {
        this.notificationListener.onNotification(notification);
    }
}
