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
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMAdapterBuilder.Factory;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.yangtools.yang.binding.Notification;

public class BindingDOMNotificationPublishServiceAdapter implements NotificationPublishService, AutoCloseable {

    static final Factory<NotificationPublishService> BUILDER_FACTORY = new BindingDOMAdapterBuilder.Factory<NotificationPublishService>() {

        @Override
        public BindingDOMAdapterBuilder<NotificationPublishService> newBuilder() {
            return new Builder();
        }

    };

    private final BindingToNormalizedNodeCodec codecRegistry;
    private final DOMNotificationPublishService domPublishService;

    public BindingDOMNotificationPublishServiceAdapter(final BindingToNormalizedNodeCodec codec, final DOMNotificationPublishService domPublishService) {
        this.codecRegistry = codec;
        this.domPublishService = domPublishService;
    }

    public BindingToNormalizedNodeCodec getCodecRegistry() {
        return codecRegistry;
    }

    public DOMNotificationPublishService getDomPublishService() {
        return domPublishService;
    }

    @Override
    public void putNotification(final Notification notification) throws InterruptedException {
        domPublishService.putNotification(toDomNotification(notification));
    }

    @Override
    public ListenableFuture<?> offerNotification(final Notification notification) {
        ListenableFuture<?> offerResult = domPublishService.offerNotification(toDomNotification(notification));
        return DOMNotificationPublishService.REJECTED.equals(offerResult)
                ? NotificationPublishService.REJECTED
                : offerResult;
    }

    @Override
    public ListenableFuture<?> offerNotification(final Notification notification, final int timeout, final TimeUnit unit) throws InterruptedException {
        ListenableFuture<?> offerResult = domPublishService.offerNotification(toDomNotification(notification), timeout, unit);
        return DOMNotificationPublishService.REJECTED.equals(offerResult)
                ? NotificationPublishService.REJECTED
                : offerResult;
    }

    private DOMNotification toDomNotification(final Notification notification) {
        return LazySerializedDOMNotification.create(codecRegistry, notification);
    }

    @Override
    public void close() throws Exception {

    }

    protected static class Builder extends BindingDOMAdapterBuilder<NotificationPublishService> {

        @Override
        public Set<Class<? extends DOMService>> getRequiredDelegates() {
            return ImmutableSet.<Class<? extends DOMService>>of(DOMNotificationPublishService.class);
        }

        @Override
        protected NotificationPublishService createInstance(final BindingToNormalizedNodeCodec codec,
                final ClassToInstanceMap<DOMService> delegates) {
            final DOMNotificationPublishService domPublish = delegates.getInstance(DOMNotificationPublishService.class);
            return new BindingDOMNotificationPublishServiceAdapter(codec, domPublish);
        }

    }
}
