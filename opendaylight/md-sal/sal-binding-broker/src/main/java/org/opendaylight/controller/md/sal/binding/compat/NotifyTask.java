/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.compat;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NotifyTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(NotifyTask.class);

    private final NotificationListenerRegistration<?> registration;
    private final Notification notification;

    public NotifyTask(final NotificationListenerRegistration<?> registration, final Notification notification) {
        this.registration = Preconditions.checkNotNull(registration);
        this.notification = Preconditions.checkNotNull(notification);
    }

    @SuppressWarnings("unchecked")
    private <T extends Notification> NotificationListenerRegistration<T> getRegistration() {
        return (NotificationListenerRegistration<T>)registration;
    }

    @Override
    public void run() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Delivering notification {} to {}", notification, registration.getInstance());
        } else {
            LOG.trace("Delivering notification {} to {}", notification.getClass().getName(), registration.getInstance());
        }

        try {
            getRegistration().notify(notification);
        } catch (final Exception e) {
            LOG.error("Unhandled exception thrown by listener: {}", registration.getInstance(), e);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Notification delivered {} to {}", notification, registration.getInstance());
        } else {
            LOG.trace("Notification delivered {} to {}", notification.getClass().getName(), registration.getInstance());
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((registration== null) ? 0 : registration.hashCode());
        result = prime * result + ((notification== null) ? 0 : notification.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NotifyTask other = (NotifyTask) obj;
        if (registration == null) {
            if (other.registration != null) {
                return false;
            }
        } else if (!registration.equals(other.registration)) {
            return false;
        }
        if (notification == null) {
            if (other.notification != null) {
                return false;
            }
        } else if (!notification.equals(other.notification)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("listener", registration)
                .add("notification", notification.getClass())
                .toString();
    }
}
