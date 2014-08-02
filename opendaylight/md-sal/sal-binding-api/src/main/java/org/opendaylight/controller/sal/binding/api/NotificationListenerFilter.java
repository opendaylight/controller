/*
 * Copyright (c) 2013 Ciena Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Marker interface to identify implementations of notification listener instance filters. An implementation of this
 * interface is associated with a specific registration and likely should be available via the registration object.
 * Implementations of this interface allow a registered listener the ability to filter incoming notifications so that
 * the are only notified (inovoked) when a matching notification is published.
 */
public interface NotificationListenerFilter {
    public boolean match(Notification notification);
}