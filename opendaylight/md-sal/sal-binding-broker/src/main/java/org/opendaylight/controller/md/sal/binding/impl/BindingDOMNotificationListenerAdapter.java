/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.Notification;

class BindingDOMNotificationListenerAdapter implements DOMNotificationListener {

    private final NotificationInvokerFactory.NotificationInvoker invoker;
    private final BindingNormalizedNodeSerializer codec;

    public BindingDOMNotificationListenerAdapter(final BindingNormalizedNodeSerializer codec, final NotificationInvokerFactory.NotificationInvoker invoker) {
        this.codec = codec;
        this.invoker = invoker;
    }

    @Override
    public void onNotification(@Nonnull final DOMNotification notification) {
        final Notification baNotification =
                codec.fromNormalizedNodeNotification(notification.getType(), notification.getBody());
        invoker.getInvocationProxy().onNotification(baNotification);
    }
}