/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.binding.util.NotificationListenerInvoker;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

class BindingDOMNotificationListenerAdapter implements DOMNotificationListener {

    private final BindingNormalizedNodeSerializer codec;
    private final NotificationListener delegate;
    private final Map<SchemaPath,NotificationListenerInvoker> invokers;

    public BindingDOMNotificationListenerAdapter(final BindingNormalizedNodeSerializer codec, final NotificationListener delegate) {
        this.codec = codec;
        this.delegate = delegate;
        this.invokers = createInvokerMapFor(delegate.getClass());
    }

    @Override
    public void onNotification(@Nonnull final DOMNotification notification) {
        final Notification baNotification = deserialize(notification);
        final QName notificationQName = notification.getType().getLastComponent();
        getInvoker(notification.getType()).invokeNotification(delegate, notificationQName, baNotification);
    }

    private Notification deserialize(final DOMNotification notification) {
        if(notification instanceof LazySerializedDOMNotification) {
            return ((LazySerializedDOMNotification) notification).getBindingData();
        }
        return codec.fromNormalizedNodeNotification(notification.getType(), notification.getBody());
    }

    private NotificationListenerInvoker getInvoker(final SchemaPath type) {
        return invokers.get(type);
    }

    protected Set<SchemaPath> getSupportedNotifications() {
        return invokers.keySet();
    }

    public static Map<SchemaPath, NotificationListenerInvoker> createInvokerMapFor(final Class<? extends NotificationListener> implClz) {
        final Map<SchemaPath, NotificationListenerInvoker> builder = new HashMap<>();
        for(final TypeToken<?> ifaceToken : TypeToken.of(implClz).getTypes().interfaces()) {
            Class<?> iface = ifaceToken.getRawType();
            if(NotificationListener.class.isAssignableFrom(iface) && BindingReflections.isBindingClass(iface)) {
                @SuppressWarnings("unchecked")
                final Class<? extends NotificationListener> listenerType = (Class<? extends NotificationListener>) iface;
                final NotificationListenerInvoker invoker = NotificationListenerInvoker.from(listenerType);
                for(final SchemaPath path : getNotificationTypes(listenerType)) {
                    builder.put(path, invoker);
                }
            }
        }
        return ImmutableMap.copyOf(builder);
    }

    private static Set<SchemaPath> getNotificationTypes(final Class<? extends NotificationListener> type) {
        // TODO: Investigate possibility and performance impact if we cache this or expose
        // it from NotificationListenerInvoker
        final Set<SchemaPath> ret = new HashSet<>();
        for(final Method method : type.getMethods()) {
            if(BindingReflections.isNotificationCallback(method)) {
                final Class<?> notification = method.getParameterTypes()[0];
                final QName name = BindingReflections.findQName(notification);
                ret.add(SchemaPath.create(true, name));
            }
        }
        return ret;
    }
}