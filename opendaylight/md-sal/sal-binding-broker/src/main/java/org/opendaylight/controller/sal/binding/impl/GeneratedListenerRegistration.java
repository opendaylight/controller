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

public class GeneratedListenerRegistration extends AbstractObjectRegistration<NotificationListener> implements ListenerRegistration<NotificationListener> {
    private final NotificationInvoker _invoker;

    public NotificationInvoker getInvoker() {
        return this._invoker;
    }

    private NotificationBrokerImpl notificationBroker;

    public GeneratedListenerRegistration(final NotificationListener instance, final NotificationInvoker invoker, final NotificationBrokerImpl broker) {
        super(instance);
        this._invoker = invoker;
        this.notificationBroker = broker;
    }

    @Override
    protected void removeRegistration() {
        this.notificationBroker.unregisterListener(this);
        this.notificationBroker = null;
        NotificationInvoker _invoker = this.getInvoker();
        _invoker.close();
    }
}
