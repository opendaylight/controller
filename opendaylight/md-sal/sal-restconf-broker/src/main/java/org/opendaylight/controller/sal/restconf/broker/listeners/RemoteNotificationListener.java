/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.broker.listeners;

import org.opendaylight.controller.sal.binding.api.NotificationListener;

public class RemoteNotificationListener implements org.opendaylight.yangtools.yang.binding.NotificationListener {

    org.opendaylight.controller.sal.binding.api.NotificationListener listener;

    public RemoteNotificationListener(NotificationListener listener){
        this.listener = listener;
    }
    public NotificationListener getListener(){
        return this.listener;
    }

}
