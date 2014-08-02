/**
 * Copyright (c) 2014 Ciena Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import java.util.Collection;
import java.util.ArrayList;
import org.opendaylight.controller.sal.binding.api.NotificationListenerFilter;
import org.opendaylight.yangtools.yang.binding.Notification;


/**
 * NotificationListenerFilter that matches notifications that match any of the node names specified
 * in the constructor.
 */
public class NameMatchingNotificationListenerFilter implements NotificationListenerFilter {

    private final Collection<String> nodeNames;

    public NameMatchingNotificationListenerFilter(Collection<String> nodeNames) {
        this.nodeNames = new ArrayList<String>(nodeNames);
    }

    @Override
    public boolean match(Notification notification) {
        String name = null; // Need a way to map to a field in a notification

        return nodeNames.contains(name);
    }
}