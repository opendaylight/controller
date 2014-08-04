/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.connect.api.MessageTransformer;
import org.opendaylight.controller.sal.connect.api.RemoteDevice;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceCommunicator;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionCapabilities;
import org.opendaylight.controller.sal.connect.netconf.sal.NetconfDeviceRpc;
import org.opendaylight.controller.sal.connect.netconf.schema.NetconfRemoteSchemaYangSourceProvider;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaContextFactory;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaResolutionException;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceRepresentation;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.PotentialSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceRegistration;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 *  This is a mediator between NetconfDeviceCommunicator and NetconfDeviceSalFacade
 */
public final class NetconfDevice implements RemoteDevice<NetconfSessionCapabilities, NetconfMessage> {

    private static final Logger logger = LoggerFactory.getLogger(NetconfDevice.class);

    public static final Function<QName, SourceIdentifier> QNAME_TO_SOURCE_ID_FUNCTION = new Function<QName, SourceIdentifier>() {
        @Override
        public SourceIdentifier apply(final QName input) {
            return new SourceIdentifier(input.getLocalName(), Optional.fromNullable(input.getFormattedRevision()));
        }
    };

    private final RemoteDeviceId id;

    private final SchemaContextFactory schemaContextFactory;
    private final RemoteDeviceHandler<NetconfSessionCapabilities> salFacade;
    private final ListeningExecutorService processingExecutor;
    private final SchemaSourceRegistry schemaRegistry;
    private final MessageTransformer<NetconfMessage> messageTransformer;
    private final NetconfStateSchemas.NetconfStateSchemasResolver stateSchemasResolver;
    private final NotificationHandler notificationHandler;
    private final List<SchemaSourceRegistration<? extends SchemaSourceRepresentation>> sourceRegistrations = Lists.newArrayList();

    public NetconfDevice(final SchemaResourcesDTO schemaResourcesDTO, final RemoteDeviceId id, final RemoteDeviceHandler<NetconfSessionCapabilities> salFacade,
                         final ExecutorService globalProcessingExecutor, final MessageTransformer<NetconfMessage> messageTransformer) {
        this.id = id;
        this.schemaRegistry = schemaResourcesDTO.getSchemaRegistry();
        this.messageTransformer = messageTransformer;
        this.schemaContextFactory = schemaResourcesDTO.getSchemaContextFactory();
        this.salFacade = salFacade;
        this.stateSchemasResolver = schemaResourcesDTO.getStateSchemasResolver();
        this.processingExecutor = MoreExecutors.listeningDecorator(globalProcessingExecutor);
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

        final NetconfDeviceRpc deviceRpc = setUpDeviceRpc(listener);
        final ListenableFuture<CheckedFuture<SchemaContext, SchemaResolutionException>> schemaParsingFuture = processingExecutor.submit(new Callable<CheckedFuture<SchemaContext, SchemaResolutionException>>() {
            @Override
            public CheckedFuture<SchemaContext, SchemaResolutionException> call() throws Exception {
                Preconditions.checkArgument(remoteSessionCapabilities.isMonitoringSupported(),
                        "%s: Netconf device does not support netconf monitoring, yang schemas cannot be acquired. Netconf device capabilities", remoteSessionCapabilities);

                final NetconfStateSchemas availableSchemas = stateSchemasResolver.resolve(deviceRpc, remoteSessionCapabilities, id);
                logger.debug("{}: Schemas exposed by ietf-netconf-monitoring: {}", id, availableSchemas.getAvailableYangSchemasQNames());

                final Collection<SourceIdentifier> requiredSources = Collections2.transform(remoteSessionCapabilities.getModuleBasedCaps(), QNAME_TO_SOURCE_ID_FUNCTION);
                final Collection<SourceIdentifier> providedSources = Collections2.transform(availableSchemas.getAvailableYangSchemasQNames(), QNAME_TO_SOURCE_ID_FUNCTION);

                addSourcesToSchemaRegistry(deviceRpc, providedSources);

                final Set<QName> requiredSourcesNotProvided = Sets.difference(remoteSessionCapabilities.getModuleBasedCaps(), availableSchemas.getAvailableYangSchemasQNames());
                if (!requiredSourcesNotProvided.isEmpty()) {
                    logger.warn("{}: Netconf device does not provide all yang models reported in hello message capabilities, required but not provided: {}", id, requiredSourcesNotProvided);
                    logger.warn("{}: Building schema context from provided sources", id);
                    return schemaContextFactory.createSchemaContext(providedSources);
                }

                return schemaContextFactory.createSchemaContext(requiredSources);
            }
        });

        Futures.addCallback(schemaParsingFuture, new FutureCallback<CheckedFuture<SchemaContext, SchemaResolutionException>>() {
            @Override
            public void onSuccess(final CheckedFuture<SchemaContext, SchemaResolutionException> futureSchemaContext) {

                Futures.addCallback(futureSchemaContext, new FutureCallback<SchemaContext>() {
                    @Override
                    public void onSuccess(final SchemaContext result) {
                        handleSalInitializationSuccess(result, remoteSessionCapabilities, deviceRpc);
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        handleSalInitializationFailure(t, listener);
                    }
                });
            }

            @Override
            public void onFailure(final Throwable t) {
                handleSalInitializationFailure(t, listener);
            }
        });
    }

    private void handleSalInitializationSuccess(final SchemaContext result, final NetconfSessionCapabilities remoteSessionCapabilities, final NetconfDeviceRpc deviceRpc) {
        updateMessageTransformer(result);
        salFacade.onDeviceConnected(result, remoteSessionCapabilities, deviceRpc);
        notificationHandler.onRemoteSchemaUp();

        logger.debug("{}: Initialization in sal successful", id);
        logger.info("{}: Netconf connector initialized successfully", id);
    }

    private void handleSalInitializationFailure(final Throwable t, final RemoteDeviceCommunicator<NetconfMessage> listener) {
        logger.error("{}: Initialization in sal failed", id, t);
        listener.close();
        salFacade.onDeviceDisconnected();
    }

    /**
     * Update initial message transformer to use retrieved schema
     * @param currentSchemaContext
     */
    private void updateMessageTransformer(final SchemaContext currentSchemaContext) {
        messageTransformer.onGlobalContextUpdated(currentSchemaContext);
    }

    private void addSourcesToSchemaRegistry(final NetconfDeviceRpc deviceRpc, final Collection<SourceIdentifier> sourceIds) {
        final NetconfRemoteSchemaYangSourceProvider yangProvider = new NetconfRemoteSchemaYangSourceProvider(id, deviceRpc);
        for (final SourceIdentifier sourceId : sourceIds) {
            sourceRegistrations.add(schemaRegistry.registerSchemaSource(yangProvider,
                    PotentialSchemaSource.create(sourceId, YangTextSchemaSource.class, PotentialSchemaSource.Costs.REMOTE_IO.getValue())));
        }
    }

    private NetconfDeviceRpc setUpDeviceRpc(final RemoteDeviceCommunicator<NetconfMessage> listener) {
       return new NetconfDeviceRpc(listener, messageTransformer);
    }

    @Override
    public void onRemoteSessionDown() {
        salFacade.onDeviceDisconnected();
        for (final SchemaSourceRegistration<? extends SchemaSourceRepresentation> sourceRegistration : sourceRegistrations) {
            sourceRegistration.close();
        }
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

    /**
     * Just a transfer object containing schema related dependencies. Injected in constructor.
     */
    public static class SchemaResourcesDTO {
        private final SchemaSourceRegistry schemaRegistry;
        private final SchemaContextFactory schemaContextFactory;
        private final NetconfStateSchemas.NetconfStateSchemasResolver stateSchemasResolver;

        public SchemaResourcesDTO(final SchemaSourceRegistry schemaRegistry, final SchemaContextFactory schemaContextFactory, final NetconfStateSchemas.NetconfStateSchemasResolver stateSchemasResolver) {
            this.schemaRegistry = Preconditions.checkNotNull(schemaRegistry);
            this.schemaContextFactory = Preconditions.checkNotNull(schemaContextFactory);
            this.stateSchemasResolver = Preconditions.checkNotNull(stateSchemasResolver);
        }

        public SchemaSourceRegistry getSchemaRegistry() {
            return schemaRegistry;
        }

        public SchemaContextFactory getSchemaContextFactory() {
            return schemaContextFactory;
        }

        public NetconfStateSchemas.NetconfStateSchemasResolver getStateSchemasResolver() {
            return stateSchemasResolver;
        }
    }
}
