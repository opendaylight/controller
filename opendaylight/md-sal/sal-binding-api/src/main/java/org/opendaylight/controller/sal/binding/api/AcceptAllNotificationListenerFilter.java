/**
 * Copyright (c) 2014 Ciena Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import org.opendaylight.controller.sal.binding.api.NotificationListenerFilter;
import org.opendaylight.yangtools.yang.binding.Notification;


/**
 * NotificationListenerFilter that matches all notifications
 */
public class AcceptAllNotificationListenerFilter implements NotificationListenerFilter {

    @Override
    public boolean match(Notification notification) {
        return true;
    }
}