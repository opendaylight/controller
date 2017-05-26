/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.compat;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.binding.util.NotificationListenerInvoker;
import org.opendaylight.yangtools.yang.common.QName;

final class NotificationInvoker implements org.opendaylight.controller.sal.binding.api.NotificationListener<Notification> {

    private final NotificationListener delegate;
    private final Map<Class<? extends Notification>,InvokerContext> invokers;


    private NotificationInvoker(final NotificationListener listener) {
        delegate = listener;
        final Map<Class<? extends Notification>, InvokerContext> builder = new HashMap<>();
        for(final TypeToken<?> ifaceToken : TypeToken.of(listener.getClass()).getTypes().interfaces()) {
            final Class<?> iface = ifaceToken.getRawType();
            if(NotificationListener.class.isAssignableFrom(iface) && BindingReflections.isBindingClass(iface)) {
                @SuppressWarnings("unchecked")
                final Class<? extends NotificationListener> listenerType = (Class<? extends NotificationListener>) iface;
                final NotificationListenerInvoker invoker = NotificationListenerInvoker.from(listenerType);
                for(final Class<? extends Notification> type : getNotificationTypes(listenerType)) {
                    builder.put(type, new InvokerContext(BindingReflections.findQName(type) , invoker));
                }
            }
        }
        invokers = ImmutableMap.copyOf(builder);
    }

    public static NotificationInvoker invokerFor(final NotificationListener listener) {
        return new NotificationInvoker(listener);
    }

    public Set<Class<? extends Notification>> getSupportedNotifications() {
        return invokers.keySet();
    }

    @Override
    public void onNotification(final Notification notification) {
        getContext(notification.getImplementedInterface()).invoke(notification);
    }

    private InvokerContext getContext(final Class<?> type) {
        return invokers.get(type);
    }

    @SuppressWarnings("unchecked")
    private static Set<Class<? extends Notification>> getNotificationTypes(final Class<? extends org.opendaylight.yangtools.yang.binding.NotificationListener> type) {
        // TODO: Investigate possibility and performance impact if we cache this or expose
        // it from NotificationListenerInvoker
        final Set<Class<? extends Notification>> ret = new HashSet<>();
        for(final Method method : type.getMethods()) {
            if(BindingReflections.isNotificationCallback(method)) {
                final Class<? extends Notification> notification = (Class<? extends Notification>) method.getParameterTypes()[0];
                ret.add(notification);
            }
        }
        return ret;
    }

    private final class InvokerContext {

        private final QName name;
        private final NotificationListenerInvoker invoker;

        private InvokerContext(final QName name, final NotificationListenerInvoker invoker) {
            this.name = name;
            this.invoker = invoker;
        }

        public void invoke(final Notification notification) {
            invoker.invokeNotification(delegate, name, notification);
        }

    }

}
