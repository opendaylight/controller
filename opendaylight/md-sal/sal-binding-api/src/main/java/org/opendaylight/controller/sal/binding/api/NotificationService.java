/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Notification;
/**
 *
 * Notification broker, which allows clients to subscribe
 * for globally published YANG-modeled notifications.
 *
 * Binding-Aware notification broker supports two styles of listeners:
 * <ul>
 * <li>Generic listener - {@link NotificationListener} - this listener interface has one callback method <code>onNotification</code>
 * which is invoked for any notification type listener is subscribed to.
 * </li>
 * <li>Dispatch listener - listener, which implements <code>{ModelName}Listener</code> interface,
 * which has dispatch methods for each defined notification. Methods are invoked based on notification type (class).
 * </li>
 *
 * <h2>Listener Types</h2>
 * <h3>Generic Listener - {@link NotificationListener}</h3>
 *
 * Generic listener is listener implementing {@link NotificationListener} interface,
 * and users are required to implement one method <code>onNotification</code>.
 * <p>
 * This listeners are registered using {@link #registerNotificationListener(Class, NotificationListener)}
 * method, which allows you to specify Notification Type and instance of listener which will be invoked.
 * <p>
 * Generic listeners allow for more modular approach, when you could subscribe just
 * to one type of notification from model, or to have general subscription
 * for all notification in system via <code>service.registerNotificationListener(Notification.class, listener).
 * <p>
 * Generic listener may be registered for multiple notification types,
 * via multiple registrations.
 *
 * <h3>Dispatch Listener</h3>
 *
 * Dispatch listeners are user-implemented listeners, which implements implements <code>{ModelName}Listener</code>
 * interfaces and are registered using {@link #registerNotificationListener(org.opendaylight.yangtools.yang.binding.NotificationListener)}
 * interface.
 *
 * Generated <code>{ModelNane}Listener</code> interface provides separate callback
 * method for notification type defined in YANG model in form <code>on{NotificationType}(NotificationType)</code>.
 * See example for more details.
 *
 * <h4>Example</h4>
 * Lets assume we have following YANG model:
 *
 * <pre>
 * module example {
 *      ...
 *
 *      notification start {
 *          ...
 *      }
 *
 *      notification stop {
 *           ...
 *      }
 * }
 * </pre>
 *
 * The generated interface will be:
 * <pre>
 * interface ExampleListener {
 *      void onStart(Start notification);
 *      void onStop(Stop notification);
 * }
 * </pre>
 *
 * User-supplied implementation of this interface which was registered by calling {@link #registerNotificationListener(org.opendaylight.yangtools.yang.binding.NotificationListener)}
 * will be invoked once someone published <code>Start</code> or <code>Stop</code> notifications
 * using {@link NotificationProviderService}.
 *
 * <ul>
 * <li><code>onStart</code> will be invoked when someone publishes <code>Start</code> notification. Actual notification
 * will be passed as first argument.</li>
 * <li><code>onStop</code> will be invoked when someone publishes <code>Stop</code> notification. Actual notification
 * will be passed as first argument.</li>
 * </ul>
 */
public interface NotificationService extends BindingAwareService {
    /**
     * Register a generic listener for specified notification type only.
     *
     * @param notificationType Interface of notification you want to receive.
     * @param listener Instance of listener which should be notified by invoker.
     * @return Registration for listener. To unregister listener invoke {@link ListenerRegistration#close()} method.
     */
    <T extends Notification> ListenerRegistration<NotificationListener<T>> registerNotificationListener(
            Class<T> notificationType, NotificationListener<T> listener);

    /**
     * Register a listener which implements generated notification interfaces derived from
     * {@link org.opendaylight.yangtools.yang.binding.NotificationListener}.
     * Listener is registered for all notifications present in implemented interfaces.
     *
     * @param listener
     * @return Registration for listener. To unregister listener invoke {@link ListenerRegistration#close()} method.
     */
    ListenerRegistration<org.opendaylight.yangtools.yang.binding.NotificationListener> registerNotificationListener(
            org.opendaylight.yangtools.yang.binding.NotificationListener listener);
}
