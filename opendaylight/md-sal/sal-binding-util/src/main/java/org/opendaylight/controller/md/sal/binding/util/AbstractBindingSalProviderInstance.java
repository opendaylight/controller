/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.util;

import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.RpcService;

public abstract class AbstractBindingSalProviderInstance<N extends NotificationProviderService,
                                                         R extends RpcProviderRegistry>
        extends AbstractBindingSalConsumerInstance<N, R>
        implements RpcProviderRegistry, NotificationProviderService {

    public AbstractBindingSalProviderInstance(final R rpcRegistry, final N notificationBroker) {
        super(rpcRegistry, notificationBroker);
    }

    @Override
    public <T extends RpcService> RpcRegistration<T> addRpcImplementation(final Class<T> type, final T implementation)
            throws IllegalStateException {
        return getRpcRegistryChecked().addRpcImplementation(type, implementation);
    }

    @Override
    public <T extends RpcService> RoutedRpcRegistration<T> addRoutedRpcImplementation(final Class<T> type,
            final T implementation) throws IllegalStateException {
        return getRpcRegistryChecked().addRoutedRpcImplementation(type, implementation);
    }

    @Override
    public void publish(final Notification notification) {
        getNotificationBrokerChecked().publish(notification);
    }

    @Override
    public void publish(final Notification notification, final ExecutorService service) {
        getNotificationBrokerChecked().publish(notification, service);
    }

    @Override
    public <L extends RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> ListenerRegistration<L> registerRouteChangeListener(
            final L listener) {
        return getRpcRegistryChecked().registerRouteChangeListener(listener);
    }

    @Override
    public ListenerRegistration<NotificationInterestListener> registerInterestListener(
            final NotificationInterestListener interestListener) {
        return getNotificationBrokerChecked().registerInterestListener(interestListener);
    }
}
