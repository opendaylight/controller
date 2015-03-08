/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMAdapterBuilder.Factory;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class ForwardedNotificationService implements NotificationService, AutoCloseable {

    public static final Factory<NotificationService> BUILDER_FACTORY = new Factory<NotificationService>() {

        @Override
        public BindingDOMAdapterBuilder<NotificationService> newBuilder() {
            return new Builder();
        }

    };
    private final BindingNormalizedNodeSerializer codec;
    private final DOMNotificationService domNotifService;
    private final NotificationInvokerFactory notificationInvokerFactory;

    public ForwardedNotificationService(BindingNormalizedNodeSerializer codec, DOMNotificationService domNotifService, NotificationInvokerFactory notificationInvokerFactory) {
        this.codec = codec;
        this.domNotifService = domNotifService;
        this.notificationInvokerFactory = notificationInvokerFactory;
    }

    @Override
    public <T extends NotificationListener> ListenerRegistration<T> registerNotificationListener(T listener) {
        final NotificationInvokerFactory.NotificationInvoker invoker = notificationInvokerFactory.invokerFor(listener);
        final DOMNotificationListener domListener = new NotificationInvokerImpl(invoker);
        final Collection<SchemaPath> schemaPaths = convertNotifTypesToSchemaPath(invoker.getSupportedNotifications());
        final ListenerRegistration<DOMNotificationListener> domRegistration =
                domNotifService.registerNotificationListener(domListener, schemaPaths);
        return new ListenerRegistrationImpl<>(listener, domRegistration);
    }



    private Collection<SchemaPath> convertNotifTypesToSchemaPath(Set<Class<? extends Notification>> notificationTypes) {
        final List<SchemaPath> schemaPaths = new ArrayList<>();
        for (Class<? extends Notification> notificationType : notificationTypes) {
            schemaPaths.add(SchemaPath.create(true, BindingReflections.findQName(notificationType)));
        }
        return schemaPaths;
    }

    @Override
    public void close() throws Exception {

    }

    private static class ListenerRegistrationImpl<T extends NotificationListener> extends AbstractListenerRegistration<T> {
        private final ListenerRegistration<?> listenerRegistration;

        public ListenerRegistrationImpl(T listener, ListenerRegistration<?> listenerRegistration) {
            super(listener);
            this.listenerRegistration = listenerRegistration;
        }

        @Override
        protected void removeRegistration() {
            listenerRegistration.close();
        }
    }

    private class NotificationInvokerImpl implements DOMNotificationListener {
        private final NotificationInvokerFactory.NotificationInvoker invoker;

        public NotificationInvokerImpl(NotificationInvokerFactory.NotificationInvoker invoker) {
            this.invoker = invoker;
        }

        @Override
        public void onNotification(@Nonnull DOMNotification notification) {
            final Notification baNotification =
                    codec.fromNormalizedNodeNotification(notification.getType(), notification.getBody());
            invoker.getInvocationProxy().onNotification(baNotification);

        }
    }

    private static class Builder extends BindingDOMAdapterBuilder<NotificationService> {


        @Override
        protected NotificationService createInstance(BindingToNormalizedNodeCodec codec,
                ClassToInstanceMap<DOMService> delegates) {
            DOMNotificationService domNotification = delegates.getInstance(DOMNotificationService.class);
            NotificationInvokerFactory invokerFactory = SingletonHolder.INVOKER_FACTORY;
            return new ForwardedNotificationService(codec.getCodecFactory(), domNotification, invokerFactory);
        }

        @Override
        public Set<? extends Class<? extends DOMService>> getRequiredDelegates() {
            return ImmutableSet.of(DOMNotificationService.class);
        }



    }
}
