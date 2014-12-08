/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import java.util.EventListener;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.md.sal.common.api.notify.NotificationPublishService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Interface for a notification service that provides publish/subscribe capabilities for YANG
 * modeled notifications. This interface is a combination of the {@link NotificationService} and
 * {@link NotificationPublishService} interfaces.
 *
 * @deprecated Please use {@link org.opendaylight.controller.md.sal.binding.api.NotificationPublishService}.
 */
@Deprecated
public interface NotificationProviderService extends NotificationService, NotificationPublishService<Notification> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(Notification notification);

    /**
     * {@inheritDoc}
     */
    @Override
    void publish(Notification notification, ExecutorService executor);

    /**
     * Registers a listener to be notified about notification subscriptions. This
     * enables a component to know when there is a notification listener subscribed
     * for a particular notification type.
     * <p>
     * On registration of this listener, the
     * {@link NotificationInterestListener#onNotificationSubscribtion(Class)} method
     * will be invoked for every notification type that currently has a notification listener
     * subscribed.
     *
     * @param interestListener the listener that will be notified when subscriptions
     *                         for new notification types occur.
     * @return a {@link ListenerRegistration} instance that should be used to unregister the listener
     *         by invoking the {@link ListenerRegistration#close()} method when no longer needed.
     */
    ListenerRegistration<NotificationInterestListener> registerInterestListener(
            NotificationInterestListener interestListener);

    /**
     * Interface for a listener interested in being notified about notification subscriptions.
     */
    public interface NotificationInterestListener extends EventListener {

        /**
         * Callback that is invoked when a notification listener subscribes for a
         * particular notification type.
         * <p>
         * This method is only called for the first subscription that occurs for a
         * particular notification type. Subsequent subscriptions for the same
         * notification type do not trigger invocation of this method.
         * <p>
         * <b>Note:</b>This callback is delivered from thread not owned by this listener,
         * all processing should be as fast as possible and implementations should
         * not do any blocking calls or block this thread.
         *
         * @param notificationType the notification type for the subscription that occurred.
         */
        void onNotificationSubscribtion(Class<? extends Notification> notificationType);
    }
}
