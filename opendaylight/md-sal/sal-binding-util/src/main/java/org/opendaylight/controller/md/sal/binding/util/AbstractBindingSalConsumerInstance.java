/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.util;

import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.RpcService;

import com.google.common.base.Preconditions;

public abstract class AbstractBindingSalConsumerInstance<D extends DataBrokerService, N extends NotificationService, R extends RpcConsumerRegistry> //
        implements //
        RpcConsumerRegistry, //
        NotificationService, //
        DataBrokerService {

    private final R rpcRegistry;
    private final N notificationBroker;
    private final D dataBroker;

    protected final R getRpcRegistry() {
        return rpcRegistry;
    }

    protected final N getNotificationBroker() {
        return notificationBroker;
    }

    protected final D getDataBroker() {
        return dataBroker;
    }

    protected final R getRpcRegistryChecked() {
        Preconditions.checkState(rpcRegistry != null,"Rpc Registry is not available.");
        return rpcRegistry;
    }

    protected final N getNotificationBrokerChecked() {
        Preconditions.checkState(notificationBroker != null,"Notification Broker is not available.");
        return notificationBroker;
    }

    protected final D getDataBrokerChecked() {
        Preconditions.checkState(dataBroker != null, "Data Broker is not available");
        return dataBroker;
    }


    protected AbstractBindingSalConsumerInstance(R rpcRegistry, N notificationBroker, D dataBroker) {
        this.rpcRegistry = rpcRegistry;
        this.notificationBroker = notificationBroker;
        this.dataBroker = dataBroker;
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

    @Override
    public DataModificationTransaction beginTransaction() {
        return getDataBrokerChecked().beginTransaction();
    }

    @Override
    @Deprecated
    public DataObject readConfigurationData(InstanceIdentifier<? extends DataObject> path) {
        return getDataBrokerChecked().readConfigurationData(path);
    }

    @Override
    public DataObject readOperationalData(InstanceIdentifier<? extends DataObject> path) {
        return getDataBrokerChecked().readOperationalData(path);
    }

    @Override
    @Deprecated
    public ListenerRegistration<DataChangeListener> registerDataChangeListener(
            InstanceIdentifier<? extends DataObject> path, DataChangeListener listener) {
        return getDataBrokerChecked().registerDataChangeListener(path, listener);
    }
}
