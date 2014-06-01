/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory.NotificationInvoker;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;

import com.google.common.base.Preconditions;

class GeneratedListenerRegistration extends AbstractObjectRegistration<NotificationListener> implements ListenerRegistration<NotificationListener> {
    private NotificationBrokerImpl notificationBroker;
    private final NotificationInvoker invoker;

    public GeneratedListenerRegistration(final NotificationListener instance, final NotificationInvoker invoker, final NotificationBrokerImpl broker) {
        super(instance);
        this.invoker = Preconditions.checkNotNull(invoker);
        this.notificationBroker = Preconditions.checkNotNull(broker);
    }

    public NotificationInvoker getInvoker() {
        // There is a race with NotificationBrokerImpl:
        // the invoker can be closed here
        return invoker;
    }

    @Override
    protected void removeRegistration() {
        notificationBroker.unregisterListener(this);
        notificationBroker = null;
        invoker.close();
    }
}
