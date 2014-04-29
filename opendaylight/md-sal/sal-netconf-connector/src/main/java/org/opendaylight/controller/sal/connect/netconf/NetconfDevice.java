/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.sal.connect.api.MessageTransformer;
import org.opendaylight.controller.sal.connect.api.RemoteDevice;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceCommunicator;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceNotificationListener;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceSalFacade;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionCapabilities;
import org.opendaylight.controller.sal.connect.netconf.sal.NetconfDeviceRpc;
import org.opendaylight.controller.sal.connect.netconf.schema.NetconfDeviceSchemaContextProvider;
import org.opendaylight.controller.sal.connect.netconf.schema.NetconfRemoteSchemaSourceProvider;
import org.opendaylight.controller.sal.connect.netconf.schema.mapping.NetconfMessageTransformer;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.opendaylight.yangtools.yang.model.util.repo.AbstractCachingSchemaSourceProvider;
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 *
 */
public final class NetconfDevice implements AutoCloseable, RemoteDevice<NetconfSessionCapabilities, NetconfMessage>,
        RemoteDeviceNotificationListener<NetconfMessage> {

    private static final Logger logger = LoggerFactory.getLogger(NetconfDevice.class);;

    private final RemoteDeviceId id;

    private final RemoteDeviceSalFacade<NetconfSessionCapabilities> salFacade;
    private final ListeningExecutorService processingExecutor;
    private final AbstractCachingSchemaSourceProvider<String, InputStream> schemaSourceProvider;
    private final MessageTransformer<NetconfMessage> messageTransformer;

    public static NetconfDevice createNetconfDevice(final RemoteDeviceId id,
            final AbstractCachingSchemaSourceProvider<String, InputStream> schemaSourceProvider,
            final ExecutorService executor, final RemoteDeviceSalFacade<NetconfSessionCapabilities> salFacade) {
        return new NetconfDevice(id, salFacade, schemaSourceProvider, executor, new NetconfMessageTransformer());
    }

    @VisibleForTesting
    protected NetconfDevice(final RemoteDeviceId id, final RemoteDeviceSalFacade<NetconfSessionCapabilities> salFacade,
            final AbstractCachingSchemaSourceProvider<String, InputStream> schemaSourceProvider,
            final ExecutorService processingExecutor, final MessageTransformer<NetconfMessage> initialMessageTransformer) {
        this.id = id;
        this.messageTransformer = initialMessageTransformer;
        this.salFacade = salFacade;
        this.schemaSourceProvider = schemaSourceProvider;
        this.processingExecutor = MoreExecutors.listeningDecorator(processingExecutor);
    }

    @Override
    public void onRemoteSessionInitialized(final NetconfSessionCapabilities remoteSessionCapabilities,
            final RemoteDeviceCommunicator<NetconfMessage> listener) {
        // SchemaContext setup has to be performed in a dedicated thread since
        // we are in a netty thread in this method
        // Yang models are being downloaded in this method and it would cause a
        // deadlock if we used the netty thread
        // http://netty.io/wiki/thread-model.html
        final ListenableFuture<?> salInitializationFuture = processingExecutor.submit(new Runnable() {
            @Override
            public void run() {
                final NetconfDeviceRpc deviceRpc = setUpDeviceRpc(remoteSessionCapabilities, listener);
                final SchemaSourceProvider<String> delegate = new NetconfRemoteSchemaSourceProvider(id, deviceRpc);
                final SchemaContextProvider schemaContextProvider = setUpSchemaContext(delegate,
                        remoteSessionCapabilities);
                updateMessageTransformer(schemaContextProvider);
                salFacade.initializeDeviceInSal(schemaContextProvider, remoteSessionCapabilities, deviceRpc);
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
                logger.error("{}: Initialization in sal failed", id, t);
                close();
            }
        });
    }

    /**
     * Change initial message transformer for a schema aware transformer
     */
    private void updateMessageTransformer(final SchemaContextProvider schemaContextProvider) {
        messageTransformer.onGlobalContextUpdated(schemaContextProvider.getSchemaContext());
    }

    private SchemaContextProvider setUpSchemaContext(final SchemaSourceProvider<String> sourceProvider, final NetconfSessionCapabilities capabilities) {
        final SchemaSourceProvider<InputStream> remoteSourceProvider = schemaSourceProvider.createInstanceFor(sourceProvider);
        return NetconfDeviceSchemaContextProvider.createRemoteSchemaContext(id, capabilities.getModuleBasedCaps(),
                remoteSourceProvider);
    }

    private NetconfDeviceRpc setUpDeviceRpc(final NetconfSessionCapabilities capHolder, final RemoteDeviceCommunicator<NetconfMessage> listener) {
        Preconditions.checkArgument(capHolder.isMonitoringSupported(), "%s: Netconf device does not support netconf monitoring, yang schemas cannot be acquired. Netconf device capabilities", capHolder);
        return new NetconfDeviceRpc(listener, messageTransformer);
    }

    @Override
    public void onRemoteSessionDown() {
        close();
    }

    @Override
    public void close() {
        logger.info("{}: Netconf connector closed", id);
        try {
            salFacade.close();
        } catch (final Exception e) {
            logger.warn("{}: Ignoring exception while closing {}", id, salFacade, e);
        }
    }

    @Override
    public void onNotification(final NetconfMessage notification) {
        final CompositeNode parsedNotification = messageTransformer.toNotification(notification);
        salFacade.publishNotification(parsedNotification);
    }
}
