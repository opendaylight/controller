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
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMAdapterBuilder.Factory;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;

public class BindingDOMNotificationServiceAdapter implements NotificationService, AutoCloseable {

    public static final Factory<NotificationService> BUILDER_FACTORY = new Factory<NotificationService>() {

        @Override
        public BindingDOMAdapterBuilder<NotificationService> newBuilder() {
            return new Builder();
        }

    };
    private final BindingNormalizedNodeSerializer codec;
    private final DOMNotificationService domNotifService;

    public BindingDOMNotificationServiceAdapter(final BindingNormalizedNodeSerializer codec, final DOMNotificationService domNotifService) {
        this.codec = codec;
        this.domNotifService = domNotifService;
    }

    @Override
    public <T extends NotificationListener> ListenerRegistration<T> registerNotificationListener(final T listener) {
        final BindingDOMNotificationListenerAdapter domListener = new BindingDOMNotificationListenerAdapter(codec, listener);
        final ListenerRegistration<BindingDOMNotificationListenerAdapter> domRegistration =
                domNotifService.registerNotificationListener(domListener, domListener.getSupportedNotifications());
        return new ListenerRegistrationImpl<>(listener, domRegistration);
    }

    @Override
    public void close() throws Exception {

    }

    private static class ListenerRegistrationImpl<T extends NotificationListener> extends AbstractListenerRegistration<T> {
        private final ListenerRegistration<?> listenerRegistration;

        public ListenerRegistrationImpl(final T listener, final ListenerRegistration<?> listenerRegistration) {
            super(listener);
            this.listenerRegistration = listenerRegistration;
        }

        @Override
        protected void removeRegistration() {
            listenerRegistration.close();
        }
    }

    private static class Builder extends BindingDOMAdapterBuilder<NotificationService> {

        @Override
        protected NotificationService createInstance(final BindingToNormalizedNodeCodec codec,
                final ClassToInstanceMap<DOMService> delegates) {
            final DOMNotificationService domNotification = delegates.getInstance(DOMNotificationService.class);
            return new BindingDOMNotificationServiceAdapter(codec.getCodecRegistry(), domNotification);
        }

        @Override
        public Set<? extends Class<? extends DOMService>> getRequiredDelegates() {
            return ImmutableSet.of(DOMNotificationService.class);
        }
    }

    public DOMNotificationService getDomService() {
        return domNotifService;
    }
}
