/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.compat;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * An aggregated listener registration. This is a result of registering an invoker which can handle multiple
 * interfaces at the same time. In order to support correct delivery, we need to maintain per-type registrations
 * which get squashed if a notification which implements multiple interfaces is encountered.
 *
 * We take care of that by implementing alternate {@link #hashCode()}/{@link #equals(Object)}, which resolve
 * to the backing aggregator.
 *
 * @param <N> Notification type
 * @param <A> Aggregator type
 */
abstract class AggregatedNotificationListenerRegistration<N extends Notification, A> extends AbstractNotificationListenerRegistration<N> {
    private final A aggregator;

    protected AggregatedNotificationListenerRegistration(final Class<? extends Notification> type, final NotificationListener<N> listener, final A aggregator) {
        super(type, listener);
        this.aggregator = Preconditions.checkNotNull(aggregator);
    }

    protected A getAggregator() {
        return aggregator;
    }

    @Override
    public int hashCode() {
        return aggregator.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!this.getClass().equals(obj.getClass())) {
            return false;
        }

        return aggregator.equals(((AggregatedNotificationListenerRegistration<?, ?>)obj).aggregator);
    }
}
