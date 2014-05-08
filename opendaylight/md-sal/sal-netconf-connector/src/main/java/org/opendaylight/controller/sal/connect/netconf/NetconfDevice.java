/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.connect.api.MessageTransformer;
import org.opendaylight.controller.sal.connect.api.RemoteDevice;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceCommunicator;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.api.SchemaContextProviderFactory;
import org.opendaylight.controller.sal.connect.api.SchemaSourceProviderFactory;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionCapabilities;
import org.opendaylight.controller.sal.connect.netconf.sal.NetconfDeviceRpc;
import org.opendaylight.controller.sal.connect.netconf.schema.NetconfDeviceSchemaProviderFactory;
import org.opendaylight.controller.sal.connect.netconf.schema.NetconfRemoteSchemaSourceProvider;
import org.opendaylight.controller.sal.connect.netconf.schema.mapping.NetconfMessageTransformer;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.opendaylight.yangtools.yang.model.util.repo.AbstractCachingSchemaSourceProvider;
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  This is a mediator between NetconfDeviceCommunicator and NetconfDeviceSalFacade
 */
public final class NetconfDevice implements RemoteDevice<NetconfSessionCapabilities, NetconfMessage> {

    private static final Logger logger = LoggerFactory.getLogger(NetconfDevice.class);

    private final RemoteDeviceId id;

    private final RemoteDeviceHandler<NetconfSessionCapabilities> salFacade;
    private final ListeningExecutorService processingExecutor;
    private final MessageTransformer<NetconfMessage> messageTransformer;
    private final SchemaContextProviderFactory schemaContextProviderFactory;
    private final SchemaSourceProviderFactory<InputStream> sourceProviderFactory;
    private final NotificationHandler notificationHandler;

    public static NetconfDevice createNetconfDevice(final RemoteDeviceId id,
            final AbstractCachingSchemaSourceProvider<String, InputStream> schemaSourceProvider,
            final ExecutorService executor, final RemoteDeviceHandler<NetconfSessionCapabilities> salFacade) {

        return new NetconfDevice(id, salFacade, executor, new NetconfMessageTransformer(),
                new NetconfDeviceSchemaProviderFactory(id), new SchemaSourceProviderFactory<InputStream>() {
                    @Override
                    public SchemaSourceProvider<InputStream> createSourceProvider(final RpcImplementation deviceRpc) {
                        return schemaSourceProvider.createInstanceFor(new NetconfRemoteSchemaSourceProvider(id,
                                deviceRpc));
                    }
                });
    }

    @VisibleForTesting
    protected NetconfDevice(final RemoteDeviceId id, final RemoteDeviceHandler<NetconfSessionCapabilities> salFacade,
            final ExecutorService processingExecutor, final MessageTransformer<NetconfMessage> messageTransformer,
            final SchemaContextProviderFactory schemaContextProviderFactory,
            final SchemaSourceProviderFactory<InputStream> sourceProviderFactory) {
        this.id = id;
        this.messageTransformer = messageTransformer;
        this.salFacade = salFacade;
        this.sourceProviderFactory = sourceProviderFactory;
        this.processingExecutor = MoreExecutors.listeningDecorator(processingExecutor);
        this.schemaContextProviderFactory = schemaContextProviderFactory;
        this.notificationHandler = new NotificationHandler(salFacade, messageTransformer, id);
    }

    @Override
    public void onRemoteSessionUp(final NetconfSessionCapabilities remoteSessionCapabilities,
                                  final RemoteDeviceCommunicator<NetconfMessage> listener) {
        // SchemaContext setup has to be performed in a dedicated thread since
        // we are in a netty thread in this method
        // Yang models are being downloaded in this method and it would cause a
        // deadlock if we used the netty thread
        // http://netty.io/wiki/thread-model.html
        logger.debug("{}: Session to remote device established with {}", id, remoteSessionCapabilities);

        final ListenableFuture<?> salInitializationFuture = processingExecutor.submit(new Runnable() {
            @Override
            public void run() {
                final NetconfDeviceRpc deviceRpc = setUpDeviceRpc(remoteSessionCapabilities, listener);
                final SchemaSourceProvider<InputStream> delegate = sourceProviderFactory.createSourceProvider(deviceRpc);
                final SchemaContextProvider schemaContextProvider = setUpSchemaContext(delegate, remoteSessionCapabilities);
                updateMessageTransformer(schemaContextProvider);
                salFacade.onDeviceConnected(schemaContextProvider, remoteSessionCapabilities, deviceRpc);
                notificationHandler.onRemoteSchemaUp();
            }
        });

        Futures.addCallback(salInitializationFuture, new FutureCallback<Object>() {
            @Override
            public void onSuccess(final Object result) {
                logger.debug("{}: Initialization in sal successful", id);
                logger.info("{}: Netconf connector initialized successfully", id);
            }

            @Override
            public void onFailure(final Throwable t) {
                // Unable to initialize device, set as disconnected
                logger.error("{}: Initialization failed", id, t);
                salFacade.onDeviceDisconnected();
            }
        });
    }

    /**
     * Update initial message transformer to use retrieved schema
     */
    private void updateMessageTransformer(final SchemaContextProvider schemaContextProvider) {
        messageTransformer.onGlobalContextUpdated(schemaContextProvider.getSchemaContext());
    }

    private SchemaContextProvider setUpSchemaContext(final SchemaSourceProvider<InputStream> sourceProvider, final NetconfSessionCapabilities capabilities) {
        return schemaContextProviderFactory.createContextProvider(capabilities.getModuleBasedCaps(), sourceProvider);
    }

    private NetconfDeviceRpc setUpDeviceRpc(final NetconfSessionCapabilities capHolder, final RemoteDeviceCommunicator<NetconfMessage> listener) {
        Preconditions.checkArgument(capHolder.isMonitoringSupported(),
                "%s: Netconf device does not support netconf monitoring, yang schemas cannot be acquired. Netconf device capabilities", capHolder);
        return new NetconfDeviceRpc(listener, messageTransformer);
    }

    @Override
    public void onRemoteSessionDown() {
        salFacade.onDeviceDisconnected();
    }

    @Override
    public void onNotification(final NetconfMessage notification) {
        notificationHandler.handleNotification(notification);
    }

    /**
     * Handles incoming notifications. Either caches them(until onRemoteSchemaUp is called) or passes to sal Facade.
     */
    private final static class NotificationHandler {

        private final RemoteDeviceHandler<?> salFacade;
        private final List<NetconfMessage> cache = new LinkedList<>();
        private final MessageTransformer<NetconfMessage> messageTransformer;
        private boolean passNotifications = false;
        private final RemoteDeviceId id;

        NotificationHandler(final RemoteDeviceHandler<?> salFacade, final MessageTransformer<NetconfMessage> messageTransformer, final RemoteDeviceId id) {
            this.salFacade = salFacade;
            this.messageTransformer = messageTransformer;
            this.id = id;
        }

        synchronized void handleNotification(final NetconfMessage notification) {
            if(passNotifications) {
                passNotification(messageTransformer.toNotification(notification));
            } else {
                cacheNotification(notification);
            }
        }

        /**
         * Forward all cached notifications and pass all notifications from this point directly to sal facade.
         */
        synchronized void onRemoteSchemaUp() {
            passNotifications = true;

            for (final NetconfMessage cachedNotification : cache) {
                passNotification(messageTransformer.toNotification(cachedNotification));
            }

            cache.clear();
        }

        private void cacheNotification(final NetconfMessage notification) {
            Preconditions.checkState(passNotifications == false);

            logger.debug("{}: Caching notification {}, remote schema not yet fully built", id, notification);
            if(logger.isTraceEnabled()) {
                logger.trace("{}: Caching notification {}", id, XmlUtil.toString(notification.getDocument()));
            }

            cache.add(notification);
        }

        private void passNotification(final CompositeNode parsedNotification) {
            logger.debug("{}: Forwarding notification {}", id, parsedNotification);
            Preconditions.checkNotNull(parsedNotification);
            salFacade.onNotification(parsedNotification);
        }

    }
}
