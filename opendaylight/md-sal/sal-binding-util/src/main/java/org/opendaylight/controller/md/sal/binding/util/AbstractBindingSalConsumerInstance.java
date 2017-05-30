/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.util;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.RpcService;

public abstract class AbstractBindingSalConsumerInstance<N extends NotificationService, R extends RpcConsumerRegistry>
        implements RpcConsumerRegistry, NotificationService {

    private final R rpcRegistry;
    private final N notificationBroker;

    protected final R getRpcRegistry() {
        return rpcRegistry;
    }

    protected final N getNotificationBroker() {
        return notificationBroker;
    }

    protected final R getRpcRegistryChecked() {
        Preconditions.checkState(rpcRegistry != null,"Rpc Registry is not available.");
        return rpcRegistry;
    }

    protected final N getNotificationBrokerChecked() {
        Preconditions.checkState(notificationBroker != null,"Notification Broker is not available.");
        return notificationBroker;
    }

    protected AbstractBindingSalConsumerInstance(R rpcRegistry, N notificationBroker) {
        this.rpcRegistry = rpcRegistry;
        this.notificationBroker = notificationBroker;
    }

    @Override
    public <T extends RpcService> T getRpcService(Class<T> module) {
        return getRpcRegistryChecked().getRpcService(module);
    }

    @Override
    public <T extends Notification> ListenerRegistration<NotificationListener<T>> registerNotificationListener(
            Class<T> notificationType, NotificationListener<T> listener) {
        return getNotificationBrokerChecked().registerNotificationListener(notificationType, listener);
    }

    @Override
    public ListenerRegistration<org.opendaylight.yangtools.yang.binding.NotificationListener> registerNotificationListener(
            org.opendaylight.yangtools.yang.binding.NotificationListener listener) {
        return getNotificationBrokerChecked().registerNotificationListener(listener);
    }
}
