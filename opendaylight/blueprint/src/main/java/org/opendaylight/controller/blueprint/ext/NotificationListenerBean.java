/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Blueprint bean corresponding to the "notification-listener" element that registers a NotificationListener
 * with the NotificationService.
 *
 * @author Thomas Pantelis
 */
public class NotificationListenerBean {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationListenerBean.class);
    static final String NOTIFICATION_LISTENER = "notification-listener";

    private Bundle bundle;
    private NotificationService notificationService;
    private NotificationListener notificationListener;
    private ListenerRegistration<?> registration;

    public void setNotificationService(final NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void setNotificationListener(final NotificationListener notificationListener) {
        this.notificationListener = notificationListener;
    }

    public void setBundle(final Bundle bundle) {
        this.bundle = bundle;
    }

    public void init() {
        LOG.debug("{}: init - registering NotificationListener {}", bundle.getSymbolicName(), notificationListener);

        registration = notificationService.registerNotificationListener(notificationListener);
    }

    public void destroy() {
        if (registration != null) {
            LOG.debug("{}: destroy - closing ListenerRegistration {}", bundle.getSymbolicName(), notificationListener);
            registration.close();
        } else {
            LOG.debug("{}: destroy - listener was not registered", bundle.getSymbolicName());
        }
    }
}
