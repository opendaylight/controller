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
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMAdapterBuilder.Factory;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class ForwardedNotificationPublishService implements NotificationPublishService, AutoCloseable {

    static final Factory<NotificationPublishService> BUILDER_FACTORY = new BindingDOMAdapterBuilder.Factory<NotificationPublishService>() {

        @Override
        public BindingDOMAdapterBuilder<NotificationPublishService> newBuilder() {
            return new Builder();
        }

    };

    private final BindingNormalizedNodeSerializer codecRegistry;
    private final DOMNotificationPublishService domPublishService;

    public ForwardedNotificationPublishService(BindingNormalizedNodeSerializer codecRegistry, DOMNotificationPublishService domPublishService) {
        this.codecRegistry = codecRegistry;
        this.domPublishService = domPublishService;
    }

    @Override
    public void putNotification(final Notification notification) throws InterruptedException {
        domPublishService.putNotification(toDomNotification(notification));
    }

    @Override
    public boolean offerNotification(final Notification notification) {
        final ListenableFuture<?> listenableFuture = domPublishService.offerNotification(toDomNotification(notification));
        return !DOMNotificationPublishService.REJECTED.equals(listenableFuture);
    }

    @Override
    public boolean offerNotification(final Notification notification, final int timeout, final TimeUnit unit) throws InterruptedException {
        final ListenableFuture<?> listenableFuture =
                domPublishService.offerNotification(toDomNotification(notification), timeout, unit);
        return !DOMNotificationPublishService.REJECTED.equals(listenableFuture);
    }

    private DOMNotification toDomNotification(final Notification notification) {
        final ContainerNode domNotification = codecRegistry.toNormalizedNodeNotification(notification);
        return new DOMNotificationImpl(domNotification);
    }

    @Override
    public void close() throws Exception {

    }

    private static class DOMNotificationImpl implements DOMNotification {

        private final SchemaPath type;
        private final ContainerNode body;

        public DOMNotificationImpl(final ContainerNode body) {
            this.type = SchemaPath.create(true, body.getIdentifier().getNodeType());
            this.body = body;
        }

        @Nonnull
        @Override
        public SchemaPath getType() {
            return this.type;
        }

        @Nonnull
        @Override
        public ContainerNode getBody() {
            return this.body;
        }
    }

    protected static class Builder extends BindingDOMAdapterBuilder<NotificationPublishService> {

        @Override
        public Set<Class<? extends DOMService>> getRequiredDelegates() {
            return ImmutableSet.<Class<? extends DOMService>>of(DOMNotificationPublishService.class);
        }

        @Override
        protected NotificationPublishService createInstance(BindingToNormalizedNodeCodec codec,
                ClassToInstanceMap<DOMService> delegates) {
            BindingNormalizedNodeSerializer codecReg = codec.getCodecRegistry();
            DOMNotificationPublishService domPublish = delegates.getInstance(DOMNotificationPublishService.class);
            return new ForwardedNotificationPublishService(codecReg, domPublish);
        }

    }
}
