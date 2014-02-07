/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.util;

import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.md.sal.common.api.RegistrationListener;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandlerRegistration;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.RpcService;

public abstract class AbstractBindingSalProviderInstance<D extends DataProviderService, N extends NotificationProviderService, R extends RpcProviderRegistry> //
        extends AbstractBindingSalConsumerInstance<D, N, R> //
        implements //
        DataProviderService, //
        RpcProviderRegistry, //
        NotificationProviderService {

    public AbstractBindingSalProviderInstance(R rpcRegistry, N notificationBroker,
            D dataBroker) {
        super(rpcRegistry, notificationBroker, dataBroker);
    }

    @Override
    public Registration<DataReader<InstanceIdentifier<? extends DataObject>, DataObject>> registerDataReader(
            InstanceIdentifier<? extends DataObject> path,
            DataReader<InstanceIdentifier<? extends DataObject>, DataObject> reader) {
        return getDataBrokerChecked().registerDataReader(path, reader);
    }

    @Override
    public Registration<DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject>> registerCommitHandler(
            InstanceIdentifier<? extends DataObject> path,
            DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> commitHandler) {
        return getDataBrokerChecked().registerCommitHandler(path, commitHandler);
    }

    @Override
    public ListenerRegistration<RegistrationListener<DataCommitHandlerRegistration<InstanceIdentifier<? extends DataObject>, DataObject>>> registerCommitHandlerListener(
            RegistrationListener<DataCommitHandlerRegistration<InstanceIdentifier<? extends DataObject>, DataObject>> commitHandlerListener) {
        return getDataBrokerChecked().registerCommitHandlerListener(commitHandlerListener);
    }

    @Override
    public <T extends RpcService> RpcRegistration<T> addRpcImplementation(Class<T> type, T implementation)
            throws IllegalStateException {
        return getRpcRegistryChecked().addRpcImplementation(type, implementation);
    }

    @Override
    public <T extends RpcService> RoutedRpcRegistration<T> addRoutedRpcImplementation(Class<T> type, T implementation)
            throws IllegalStateException {
        return getRpcRegistryChecked().addRoutedRpcImplementation(type, implementation);
    }

    @Override
    @Deprecated
    public void notify(Notification notification) {
        getNotificationBrokerChecked().notify(notification);
    }

    @Override
    @Deprecated
    public void notify(Notification notification, ExecutorService service) {
        getNotificationBrokerChecked().notify(notification, service);
    }

    @Override
    public void publish(Notification notification) {
        getNotificationBrokerChecked().publish(notification);
    }

    @Override
    public void publish(Notification notification, ExecutorService service) {
        getNotificationBrokerChecked().publish(notification, service);
    }

    @Override
    public <L extends RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> ListenerRegistration<L> registerRouteChangeListener(
            L listener) {
        return getRpcRegistryChecked().registerRouteChangeListener(listener);
    }

    @Override
    public ListenerRegistration<NotificationInterestListener> registerInterestListener(
            NotificationInterestListener interestListener) {
        return getNotificationBrokerChecked().registerInterestListener(interestListener);
    }
}
