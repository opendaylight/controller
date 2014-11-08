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

public class RemoteNotificationListener<T extends Notification> implements org.opendaylight.yangtools.yang.binding.NotificationListener {

    NotificationListener<T> listener;

    public RemoteNotificationListener(NotificationListener<T> listener){
        this.listener = listener;
    }
    public NotificationListener<T> getListener() {
        return this.listener;
    }

}
