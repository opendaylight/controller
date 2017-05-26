/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.compat;

import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.impl.LazySerializedDOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.Notification;

final class FunctionalNotificationListenerAdapter<N extends Notification> implements DOMNotificationListener {

    private final BindingNormalizedNodeSerializer codec;
    private final NotificationListener<N> delegate;
    private final Class<N> type;

    public FunctionalNotificationListenerAdapter(final BindingNormalizedNodeSerializer codec, final Class<N> type, final NotificationListener<N> delegate) {
        this.codec = codec;
        this.type = type;
        this.delegate = delegate;
    }

    @Override
    public void onNotification(@Nonnull final DOMNotification notification) {
        delegate.onNotification( type.cast(deserialize(notification)));
    }

    private Notification deserialize(final DOMNotification notification) {
        if(notification instanceof LazySerializedDOMNotification) {
            return ((LazySerializedDOMNotification) notification).getBindingData();
        }
        return codec.fromNormalizedNodeNotification(notification.getType(), notification.getBody());
    }
}
