/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import java.util.concurrent.Callable;

import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

class NotifyTask implements Callable<Object> {
    private static final Logger LOG = LoggerFactory.getLogger(NotifyTask.class);

    private final NotificationListener<?> listener;
    private final Notification notification;

    public NotifyTask(final NotificationListener<?> listener, final Notification notification) {
        this.listener = Preconditions.checkNotNull(listener);
        this.notification = Preconditions.checkNotNull(notification);
    }

    @SuppressWarnings("unchecked")
    private <T extends Notification> NotificationListener<T> getListener() {
        return (NotificationListener<T>)listener;
    }

    @Override
    public Object call() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Delivering notification {} to {}", notification, listener);
        } else {
            LOG.trace("Delivering notification {} to {}", notification.getClass().getName(), listener);
        }

        try {
            getListener().onNotification(notification);
        } catch (final Exception e) {
            LOG.error("Unhandled exception thrown by listener: {}", listener, e);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Notification delivered {} to {}", notification, listener);
        } else {
            LOG.trace("Notification delivered {} to {}", notification.getClass().getName(), listener);
        }

        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((listener== null) ? 0 : listener.hashCode());
        result = prime * result + ((notification== null) ? 0 : notification.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NotifyTask other = (NotifyTask) obj;
        if (listener == null) {
            if (other.listener != null)
                return false;
        } else if (!listener.equals(other.listener))
            return false;
        if (notification == null) {
            if (other.notification != null)
                return false;
        } else if (!notification.equals(other.notification))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("listener", listener)
                .add("notification", notification.getClass())
                .toString();
    }
}
