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
 *
 * Notification Broker which provides publish/subscribe capability for YANG
 * modeled notifications.
 *
 * <p>
 * This notification broker has same subscribe features as
 * {@link NotificationService}. See {@link NotificationService} if you want to
 * subscribe notifications.
 *
 * <h2>Publishing notifications</h2>
 *
 * Users of this interface are able to publish any YANG-modeled notification to
 * broker, which will be delivered to all interested listeners in same broker.
 * <p>
 * Publication of notifications is done by invoking
 * {@link #publish(Notification)} or
 * {@link #publish(Notification, ExecutorService)}.
 * <p>
 * Metadata required to deliver notification to correct listeners, are extracted
 * from published notification by broker. Set of listeners which will be
 * notified is derived by broker, based on existing notification listeners.
 *
 */
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
     *
     * Register listener, which gets notified about incoming request
     * for notification subscription.
     *
     * <p>
     * This listener is invoked, which will be notified when subscription
     * for new notification type is created. This provides listener
     * with information that there is listener registered to this broker,
     * which is interested in receiving particular notification type.
     * <p>
     * During registration of this listener, method {@link NotificationInterestListener#onNotificationSubscribtion(Class)}
     * will be invoked for every notification type for which listeners
     * were registered before this listener was registered.
     *
     *
     *
     * @param interestListener Listener which will be notified when subscription
     * for new notification type is created.
     * @return Listener registration uniquely identifying registration of listener.
     * Invoke <code>close()</code> method on this object, to stop receiving
     * this notifications.
     */
    ListenerRegistration<NotificationInterestListener> registerInterestListener(
            NotificationInterestListener interestListener);

    public interface NotificationInterestListener extends EventListener {

        /**
         * Callback which is invoked when there is listener registering
         * for particular notification type.
         *
         * This callback is not registered for every registration for particular
         * notification type, but only for first registration. Subsequent
         * listener registrations for same notification type  do not trigger
         * invocation of this method for same type.
         *
         * <b>Note:</b>This callback is delivered from thread not owned by this listener,
         * all processing should be as fast as possible and implementations should
         * not do any blocking calls or block this thread.
         *
         */
        void onNotificationSubscribtion(Class<? extends Notification> notificationType);
    }
}
