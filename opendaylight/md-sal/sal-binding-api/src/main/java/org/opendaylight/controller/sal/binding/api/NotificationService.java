/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import org.opendaylight.yangtools.yang.binding.Notification;

public interface NotificationService extends BindingAwareService {

    <T extends Notification> void addNotificationListener(Class<T> notificationType, NotificationListener<T> listener);

    <T extends Notification> void removeNotificationListener(Class<T> notificationType, NotificationListener<T> listener);
}
