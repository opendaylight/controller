/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.compat;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.common.api.RegistrationListener;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandlerRegistration;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareService;
import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderInstance;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.RpcService;

@Deprecated
public class HydrogenMountInstanceAdapter implements MountProviderInstance {

    private final ClassToInstanceMap<BindingAwareService> services;
    private final InstanceIdentifier<?> identifier;


    public HydrogenMountInstanceAdapter(final MountPoint key) {
        this.identifier = key.getIdentifier();
        final ImmutableClassToInstanceMap.Builder<BindingAwareService> builder = ImmutableClassToInstanceMap.builder();

        final Optional<DataBroker> dataBroker = key.getService(DataBroker.class);
        if(dataBroker.isPresent()) {
            builder.put(DataBrokerService.class, new HydrogenDataBrokerAdapter(dataBroker.get()));
        }
        final Optional<org.opendaylight.controller.md.sal.binding.api.NotificationService> notificationService = key.getService(org.opendaylight.controller.md.sal.binding.api.NotificationService.class);
        if(notificationService.isPresent()) {
            builder.put(NotificationService.class, new HeliumNotificationServiceAdapter(notificationService.get()));
        }

        services = builder.build();
    }


    private <T extends BindingAwareService> T service(final Class<T> service) {
        final T potential = services.getInstance(service);
        Preconditions.checkState(potential != null, "Service %s is not supported by mount point %s",service,this.getIdentifier());
        return potential;
    }

    @Override
    public <T extends RpcService> T getRpcService(final Class<T> serviceInterface) {
        return service(RpcConsumerRegistry.class).getRpcService(serviceInterface);
    }

    @Override
    public InstanceIdentifier<?> getIdentifier() {
        return identifier;
    }

    @Override
    public <T extends Notification> ListenerRegistration<NotificationListener<T>> registerNotificationListener(
            final Class<T> notificationType, final NotificationListener<T> listener) {
        return service(NotificationService.class).registerNotificationListener(notificationType, listener);
    }

    @Override
    public ListenerRegistration<org.opendaylight.yangtools.yang.binding.NotificationListener> registerNotificationListener(
            final org.opendaylight.yangtools.yang.binding.NotificationListener listener) {
        return service(NotificationService.class).registerNotificationListener(listener);
    }

    @Override
    public DataModificationTransaction beginTransaction() {
        return service(DataBrokerService.class).beginTransaction();
    }

    @Override
    public DataObject readConfigurationData(final InstanceIdentifier<? extends DataObject> path) {
        return service(DataBrokerService.class).readConfigurationData(path);
    }

    @Override
    public DataObject readOperationalData(final InstanceIdentifier<? extends DataObject> path) {
        return service(DataBrokerService.class).readOperationalData(path);
    }

    @Override
    public ListenerRegistration<DataChangeListener> registerDataChangeListener(
            final InstanceIdentifier<? extends DataObject> path, final DataChangeListener listener) {
        return service(DataBrokerService.class).registerDataChangeListener(path,listener);
    }

    @Override
    public <T extends RpcService> RoutedRpcRegistration<T> addRoutedRpcImplementation(final Class<T> serviceInterface,
            final T implementation) throws IllegalStateException {
        return service(RpcProviderRegistry.class).addRoutedRpcImplementation(serviceInterface, implementation);
    }

    @Override
    public <T extends RpcService> RpcRegistration<T> addRpcImplementation(final Class<T> serviceInterface, final T implementation)
            throws IllegalStateException {
        return service(RpcProviderRegistry.class).addRpcImplementation(serviceInterface, implementation);
    }

    @Override
    public void publish(final Notification notification) {
        service(NotificationProviderService.class).publish(notification);
    }

    @Override
    public void publish(final Notification notification, final ExecutorService executor) {
        service(NotificationProviderService.class).publish(notification);
    }

    @Override
    public Registration registerCommitHandler(final InstanceIdentifier<? extends DataObject> arg0,
            final DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> arg1) {
        return service(DataProviderService.class).registerCommitHandler(arg0, arg1);
    }

    @Override
    public ListenerRegistration<RegistrationListener<DataCommitHandlerRegistration<InstanceIdentifier<? extends DataObject>, DataObject>>> registerCommitHandlerListener(
            final RegistrationListener<DataCommitHandlerRegistration<InstanceIdentifier<? extends DataObject>, DataObject>> arg0) {
        return service(DataProviderService.class).registerCommitHandlerListener(arg0);
    }

    @Override
    public Registration registerDataReader(final InstanceIdentifier<? extends DataObject> path,
            final DataReader<InstanceIdentifier<? extends DataObject>, DataObject> reader) {
        return service(DataProviderService.class).registerDataReader(path, reader);
    }

    @Override
    public ListenerRegistration<NotificationInterestListener> registerInterestListener(
            final NotificationInterestListener interestListener) {
        return service(NotificationProviderService.class).registerInterestListener(interestListener);
    }

    @Override
    public <L extends RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> ListenerRegistration<L> registerRouteChangeListener(
            final L arg0) {
        return service(RpcProviderRegistry.class).registerRouteChangeListener(arg0);
    }

}
