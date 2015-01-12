/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker;

import org.opendaylight.controller.sal.core.api.notify.NotificationListener;
import org.opendaylight.controller.sal.core.api.notify.NotificationPublishService;
import org.opendaylight.controller.sal.dom.broker.impl.NotificationRouterImpl;
import org.opendaylight.controller.sal.dom.broker.spi.NotificationRouter;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public class SimpleNotificationPublishService implements NotificationPublishService {
        private final NotificationRouter notificationRouter;

        public SimpleNotificationPublishService(final NotificationRouter notificationRouter) {
            this.notificationRouter = notificationRouter;
        }

        public SimpleNotificationPublishService() {
            this(new NotificationRouterImpl());
        }

        @Override
        public void publish(final CompositeNode notification) {
            notificationRouter.publish(notification);
        }

        @Override
        public ListenerRegistration<NotificationListener> addNotificationListener(final QName notification, final NotificationListener listener) {
            return notificationRouter.addNotificationListener(notification, listener);
        }
    }