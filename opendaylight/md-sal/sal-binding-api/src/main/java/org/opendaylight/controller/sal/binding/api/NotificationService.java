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
 * Notification broker which allows clients to subscribe for and publish YANG-modeled notifications.
 *
 *<p>
 * Two styles of listeners are supported:
 * <ul>
 * <li>Generic listener</li>
 * <li>Dispatch listener - listener, which implements <code>{ModelName}Listener</code> interface,
 * which has dispatch methods for each defined notification. Methods are invoked based on notification type (class).
 * </li>
 * </ul>
 *
 * <h3>Generic Listener</h3>
 * <p>
 * A generic listener implements the {@link NotificationListener} interface which has one callback method
 * <code>onNotification</code> that is invoked for any notification type the listener is subscribed to.
 * <p>
 * A generic listener is subscribed using the {@link #registerNotificationListener(Class, NotificationListener)}
 * method by which you specify the type of notification to receive. A generic listener may be registered for
 * multiple notification types via multiple subscriptions.
 * <p>
 * Generic listeners allow for a more flexible approach, allowing you to subscribe for just
 * one type of notification from a YANG model. You could also have a general subscription
 * for all notification in the system via
 * <pre>
 *   service.registerNotificationListener(Notification.class, listener);
 * </pre>
 *
 * <h3>Dispatch Listener</h3>
 * <p>
 * A dispatch listener implements a YANG-generated module interface <code>{ModuleName}Listener</code>
 * which handles all the notifications defined in the YANG model. Each notification type translates to
 * a specific method of the form <code>on{NotificationType}</code> on the generated interface.
 * The generated interface also extends the
 * {@link org.opendaylight.yangtools.yang.binding.NotificationListener} interface and implementations
 * are registered using {@link #registerNotificationListener(org.opendaylight.yangtools.yang.binding.NotificationListener)}
 * method.
 *
 * <h4>Dispatch Listener Example</h4>
 * <p>
 * Lets assume we have following YANG model:
 *
 * {@code
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
 * }
 *
 * The generated interface will be:
 * {@code
 *  public interface ExampleListener extends NotificationListener {
 *      void onStart(Start notification);
 *      void onStop(Stop notification);
 *  }
 * }
 * The following defines an implementation of the generated interface:
 * {@code
 *  public class MyExampleListener implements ExampleListener {
 *      public void onStart(Start notification) {
 *          // do something
 *      }
 *
 *      public void onStop(Stop notification) {
 *          // do something
 *      }
 *  }
 * }
 * The implementation is registered as follows:
 * {@code
 *  MyExampleListener listener = new MyExampleListener();
 *  ListenerRegistration<NotificationListener> reg = service.registerNotificationListener( listener );
 * }
 * The <code>onStart</code> method will be invoked when someone publishes a <code>Start</code> notification and
 * the <code>onStop</code> method will be invoked when someone publishes a <code>Stop</code> notification.
 *
 * @deprecated Please use {@link org.opendaylight.controller.md.sal.binding.api.NotificationService} instead.
 */
@Deprecated
public interface NotificationService extends BindingAwareService {
    /**
     * Registers a generic listener implementation for a specified notification type.
     *
     * @param notificationType the YANG-generated interface of the notification type.
     * @param listener the listener implementation that will receive notifications.
     * @return a {@link ListenerRegistration} instance that should be used to unregister the listener
     *         by invoking the {@link ListenerRegistration#close()} method when no longer needed.
     */
    <T extends Notification> ListenerRegistration<NotificationListener<T>> registerNotificationListener(
            Class<T> notificationType, NotificationListener<T> listener);

    /**
     * Registers a listener which implements a YANG-generated notification interface derived from
     * {@link org.opendaylight.yangtools.yang.binding.NotificationListener}.
     * The listener is registered for all notifications present in the implemented interface.
     *
     * @param listener the listener implementation that will receive notifications.
     * @return a {@link ListenerRegistration} instance that should be used to unregister the listener
     *         by invoking the {@link ListenerRegistration#close()} method when no longer needed.
     */
    ListenerRegistration<org.opendaylight.yangtools.yang.binding.NotificationListener> registerNotificationListener(
            org.opendaylight.yangtools.yang.binding.NotificationListener listener);
}
