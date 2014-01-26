/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.spi;

import java.util.Set;

import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.yangtools.yang.binding.Notification;

public interface NotificationInvokerFactory {

    NotificationInvoker invokerFor(org.opendaylight.yangtools.yang.binding.NotificationListener instance);

    public interface NotificationInvoker {

        Set<Class<? extends Notification>> getSupportedNotifications();

        NotificationListener<Notification> getInvocationProxy();

        public abstract void close();

        org.opendaylight.yangtools.yang.binding.NotificationListener getDelegate();

    }
}
