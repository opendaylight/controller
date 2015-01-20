/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.sal;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCapabilities;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.notify.NotificationListener;
import org.opendaylight.controller.sal.core.api.notify.NotificationPublishService;
import org.opendaylight.controller.sal.dom.broker.impl.NotificationRouterImpl;
import org.opendaylight.controller.sal.dom.broker.impl.SchemaAwareRpcBroker;
import org.opendaylight.controller.sal.dom.broker.spi.NotificationRouter;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetconfDeviceSalFacade implements AutoCloseable, RemoteDeviceHandler<NetconfSessionPreferences> {

    private static final Logger logger= LoggerFactory.getLogger(NetconfDeviceSalFacade.class);

    private final RemoteDeviceId id;
    private final NetconfDeviceSalProvider salProvider;

    private final List<AutoCloseable> salRegistrations = Lists.newArrayList();

    public NetconfDeviceSalFacade(final RemoteDeviceId id, final Broker domBroker, final BindingAwareBroker bindingBroker, final BundleContext bundleContext, final ExecutorService executor) {
        this.id = id;
        this.salProvider = new NetconfDeviceSalProvider(id, executor);
        registerToSal(domBroker, bindingBroker, bundleContext);
    }

    public void registerToSal(final Broker domRegistryDependency, final BindingAwareBroker bindingBroker, final BundleContext bundleContext) {
        domRegistryDependency.registerProvider(salProvider, bundleContext);
        bindingBroker.registerProvider(salProvider, bundleContext);
    }

    @Override
    public synchronized void onNotification(final CompositeNode domNotification) {
        salProvider.getMountInstance().publish(domNotification);
    }

    @Override
    public synchronized void onDeviceConnected(final SchemaContext schemaContext,
                                               final NetconfSessionPreferences netconfSessionPreferences, final RpcImplementation deviceRpc) {

        // TODO move SchemaAwareRpcBroker from sal-broker-impl, now we have depend on the whole sal-broker-impl
        final RpcProvisionRegistry rpcRegistry = new SchemaAwareRpcBroker(id.getPath().toString(), new SchemaContextProvider() {
            @Override
            public SchemaContext getSchemaContext() {
                return schemaContext;
            }
        });
        registerRpcsToSal(schemaContext, rpcRegistry, deviceRpc);
        final DOMDataBroker domBroker = new NetconfDeviceDataBroker(id, deviceRpc, schemaContext, netconfSessionPreferences);

        // TODO NotificationPublishService and NotificationRouter have the same interface
        final NotificationPublishService notificationService = new NotificationPublishService() {

            private final NotificationRouter innerRouter = new NotificationRouterImpl();

            @Override
            public void publish(final CompositeNode notification) {
                innerRouter.publish(notification);
            }

            @Override
            public ListenerRegistration<NotificationListener> addNotificationListener(final QName notification, final NotificationListener listener) {
                return innerRouter.addNotificationListener(notification, listener);
            }
        };

        salProvider.getMountInstance().onDeviceConnected(schemaContext, domBroker, rpcRegistry, notificationService);
        salProvider.getDatastoreAdapter().updateDeviceState(true, netconfSessionPreferences.getModuleBasedCaps());
        salProvider.getTopologyDatastoreAdapter().updateDeviceData(true, netconfSessionPreferences.getNetconfDeviceCapabilities());
    }

    @Override
    public synchronized void onDeviceDisconnected() {
        salProvider.getDatastoreAdapter().updateDeviceState(false, Collections.<QName>emptySet());
        salProvider.getTopologyDatastoreAdapter().updateDeviceData(false, new NetconfDeviceCapabilities());
        salProvider.getMountInstance().onDeviceDisconnected();
    }

    @Override
    public void onDeviceFailed(Throwable throwable) {
        salProvider.getTopologyDatastoreAdapter().setDeviceAsFailed(throwable);
        salProvider.getMountInstance().onDeviceDisconnected();
    }

    private void registerRpcsToSal(final SchemaContext schemaContext, final RpcProvisionRegistry rpcRegistry, final RpcImplementation deviceRpc) {
        final Map<QName, String> failedRpcs = Maps.newHashMap();
        for (final RpcDefinition rpcDef : schemaContext.getOperations()) {
            try {
                salRegistrations.add(rpcRegistry.addRpcImplementation(rpcDef.getQName(), deviceRpc));
                logger.debug("{}: Rpc {} from netconf registered successfully", id, rpcDef.getQName());
            } catch (final Exception e) {
                // Only debug per rpc, warn for all of them at the end to pollute log a little less (e.g. routed rpcs)
                logger.debug("{}: Unable to register rpc {} from netconf device. This rpc will not be available", id,
                        rpcDef.getQName(), e);
                failedRpcs.put(rpcDef.getQName(), e.getClass() + ":" + e.getMessage());
            }
        }

        if (failedRpcs.isEmpty() == false) {
            if (logger.isDebugEnabled()) {
                logger.warn("{}: Some rpcs from netconf device were not registered: {}", id, failedRpcs);
            } else {
                logger.warn("{}: Some rpcs from netconf device were not registered: {}", id, failedRpcs.keySet());
            }
        }
    }

    @Override
    public void close() {
        for (final AutoCloseable reg : Lists.reverse(salRegistrations)) {
            closeGracefully(reg);
        }
        closeGracefully(salProvider);
    }

    private void closeGracefully(final AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (final Exception e) {
                logger.warn("{}: Ignoring exception while closing {}", id, resource, e);
            }
        }
    }
}
