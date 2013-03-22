/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api.notify;

import java.util.Set;

import org.opendaylight.controller.sal.core.api.Consumer;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;


/**
 * Notification listener for SAL notifications.
 */
public interface NotificationListener extends Consumer.ConsumerFunctionality {
    /**
     * A set of notification types supported by listeners.
     * 
     * The set of notification {@link QName}s which are supported by this
     * listener. This set is used, when {@link Consumer} is registered to the
     * SAL, to automatically register the listener.
     * 
     * @return Set of QNames identifying supported notifications.
     */
    Set<QName> getSupportedNotifications();

    /**
     * Fired when the notification occurs.
     * 
     * The type of the notification could be learned by
     * <code>QName type = notification.getNodeType();</code>
     * 
     * @param notification
     *            Notification content
     */
    void onNotification(CompositeNode notification);
}
