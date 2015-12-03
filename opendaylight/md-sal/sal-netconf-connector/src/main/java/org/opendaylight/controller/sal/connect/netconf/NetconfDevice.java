/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf;

import com.google.common.annotations.VisibleForTesting;
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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.sal.connect.api.MessageTransformer;
import org.opendaylight.controller.sal.connect.api.RemoteDevice;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceCommunicator;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCapabilities;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCommunicator;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.connect.netconf.sal.NetconfDeviceRpc;
import org.opendaylight.controller.sal.connect.netconf.schema.NetconfRemoteSchemaYangSourceProvider;
import org.opendaylight.controller.sal.connect.netconf.schema.mapping.NetconfMessageTransformer;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.extension.rev131210.$YangModuleInfoImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.NetconfCapabilityChange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.fields.unavailable.capabilities.UnavailableCapability;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.repo.api.MissingSchemaSourceException;
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

/**
 *  This is a mediator between NetconfDeviceCommunicator and NetconfDeviceSalFacade
 */
public final class NetconfDevice implements RemoteDevice<NetconfSessionPreferences, NetconfMessage, NetconfDeviceCommunicator> {

    private static final Logger logger = LoggerFactory.getLogger(NetconfDevice.class);

    /**
     * Initial schema context contains schemas for netconf monitoring and netconf notifications
     */
    public static final SchemaContext INIT_SCHEMA_CTX;

    static {
        try {
            final ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();
            moduleInfoBackedContext.addModuleInfos(
                    Lists.newArrayList(
                            $YangModuleInfoImpl.getInstance(),
                            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.$YangModuleInfoImpl.getInstance(),
                            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.$YangModuleInfoImpl.getInstance()));
            INIT_SCHEMA_CTX = moduleInfoBackedContext.tryToCreateSchemaContext().get();
        } catch (final RuntimeException e) {
            logger.error("Unable to prepare schema context for netconf initialization", e);
            throw new ExceptionInInitializerError(e);
        }
    }

    public static final Function<QName, SourceIdentifier> QNAME_TO_SOURCE_ID_FUNCTION = new Function<QName, SourceIdentifier>() {
        @Override
        public SourceIdentifier apply(final QName input) {
            return new SourceIdentifier(input.getLocalName(), Optional.fromNullable(input.getFormattedRevision()));
        }
    };

    private final RemoteDeviceId id;
    private final boolean reconnectOnSchemasChange;

    private final SchemaContextFactory schemaContextFactory;
    private final RemoteDeviceHandler<NetconfSessionPreferences> salFacade;
    private final ListeningExecutorService processingExecutor;
    private final SchemaSourceRegistry schemaRegistry;
    private final NetconfStateSchemas.NetconfStateSchemasResolver stateSchemasResolver;
    private final NotificationHandler notificationHandler;
    private final List<SchemaSourceRegistration<? extends SchemaSourceRepresentation>> sourceRegistrations = Lists.newArrayList();

    // 2 minutes keepalive timeout by default
    private static final long DEFAULT_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(30);
    private final long keepaliveTimeoutMillis;

    // Message transformer is constructed once the schemas are available
    private MessageTransformer<NetconfMessage> messageTransformer;

    public NetconfDevice(final SchemaResourcesDTO schemaResourcesDTO, final RemoteDeviceId id, final RemoteDeviceHandler<NetconfSessionPreferences> salFacade,
                         final ExecutorService globalProcessingExecutor) {
        this(schemaResourcesDTO, id, salFacade, globalProcessingExecutor, false, DEFAULT_TIMEOUT_MILLIS);
    }

    public NetconfDevice(final SchemaResourcesDTO schemaResourcesDTO, final RemoteDeviceId id, final RemoteDeviceHandler<NetconfSessionPreferences> salFacade,
            final ExecutorService globalProcessingExecutor, final boolean reconnectOnSchemasChange) {
        this(schemaResourcesDTO, id, salFacade, globalProcessingExecutor, reconnectOnSchemasChange, DEFAULT_TIMEOUT_MILLIS);
}

    /**
     * Create rpc implementation capable of handling RPC for monitoring and notifications even before the schemas of remote device are downloaded
     */
    static NetconfDeviceRpc getRpcForInitialization(final NetconfDeviceCommunicator listener) {
        return new NetconfDeviceRpc(INIT_SCHEMA_CTX, listener, new NetconfMessageTransformer(INIT_SCHEMA_CTX, false));
    }


    // FIXME reduce parameters
    public NetconfDevice(final SchemaResourcesDTO schemaResourcesDTO, final RemoteDeviceId id, final RemoteDeviceHandler<NetconfSessionPreferences> salFacade,
                         final ExecutorService globalProcessingExecutor, final boolean reconnectOnSchemasChange, final long keepaliveTimeoutMillis) {
        this.id = id;
        this.reconnectOnSchemasChange = reconnectOnSchemasChange;
        this.schemaRegistry = schemaResourcesDTO.getSchemaRegistry();
        this.schemaContextFactory = schemaResourcesDTO.getSchemaContextFactory();
        this.salFacade = salFacade;
        this.stateSchemasResolver = schemaResourcesDTO.getStateSchemasResolver();
        this.processingExecutor = MoreExecutors.listeningDecorator(globalProcessingExecutor);
        this.notificationHandler = new NotificationHandler(salFacade, id);
        this.keepaliveTimeoutMillis = keepaliveTimeoutMillis;
    }

    @Override
    public void onRemoteSessionUp(final NetconfSessionPreferences remoteSessionCapabilities,
                                  final NetconfDeviceCommunicator listener) {
        // SchemaContext setup has to be performed in a dedicated thread since
        // we are in a netty thread in this method
        // Yang models are being downloaded in this method and it would cause a
        // deadlock if we used the netty thread
        // http://netty.io/wiki/thread-model.html
        logger.debug("{}: Session to remote device established with {}", id, remoteSessionCapabilities);

        final NetconfDeviceRpc initRpc = getRpcForInitialization(listener);
        final DeviceSourcesResolver task = new DeviceSourcesResolver(remoteSessionCapabilities, id, stateSchemasResolver, initRpc);
        final ListenableFuture<DeviceSources> sourceResolverFuture = processingExecutor.submit(task);

        if(shouldListenOnSchemaChange(remoteSessionCapabilities)) {
           registerToBaseNetconfStream(initRpc, listener);
        }

        final FutureCallback<DeviceSources> resolvedSourceCallback = new FutureCallback<DeviceSources>() {
            @Override
            public void onSuccess(final DeviceSources result) {
                addProvidedSourcesToSchemaRegistry(initRpc, result);
                setUpSchema(result);
            }

            private void setUpSchema(final DeviceSources result) {
                processingExecutor.submit(new RecursiveSchemaSetup(result, remoteSessionCapabilities, listener));
            }

            @Override
            public void onFailure(final Throwable t) {
                logger.warn("{}: Unexpected error resolving device sources: {}", id, t);
                handleSalInitializationFailure(t, listener);
            }
        };

        Futures.addCallback(sourceResolverFuture, resolvedSourceCallback);
    }

    private void registerToBaseNetconfStream(final NetconfDeviceRpc deviceRpc, final NetconfDeviceCommunicator listener) {
       // TODO check whether the model describing create subscription is present in schema
        // Perhaps add a default schema context to support create-subscription if the model was not provided (same as what we do for base netconf operations in transformer)
       final CheckedFuture<DOMRpcResult, DOMRpcException> rpcResultListenableFuture =
                deviceRpc.invokeRpc(NetconfMessageTransformUtil.toPath(NetconfMessageTransformUtil.CREATE_SUBSCRIPTION_RPC_QNAME), NetconfMessageTransformUtil.CREATE_SUBSCRIPTION_RPC_CONTENT);

        final NotificationHandler.NotificationFilter filter = new NotificationHandler.NotificationFilter() {
            @Override
            public Optional<DOMNotification> filterNotification(final DOMNotification notification) {
                if (isCapabilityChanged(notification)) {
                    logger.info("{}: Schemas change detected, reconnecting", id);
                    // Only disconnect is enough, the reconnecting nature of the connector will take care of reconnecting
                    listener.disconnect();
                    return Optional.absent();
                }
                return Optional.of(notification);
            }

            private boolean isCapabilityChanged(final DOMNotification notification) {
                return notification.getBody().getNodeType().equals(NetconfCapabilityChange.QNAME);
            }
        };

        Futures.addCallback(rpcResultListenableFuture, new FutureCallback<DOMRpcResult>() {
            @Override
            public void onSuccess(final DOMRpcResult domRpcResult) {
                notificationHandler.addNotificationFilter(filter);
            }

            @Override
            public void onFailure(final Throwable t) {
                logger.warn("Unable to subscribe to base notification stream. Schemas will not be reloaded on the fly", t);
            }
        });
    }

    private boolean shouldListenOnSchemaChange(final NetconfSessionPreferences remoteSessionCapabilities) {
        return remoteSessionCapabilities.isNotificationsSupported() && reconnectOnSchemasChange;
    }

    @VisibleForTesting
    void handleSalInitializationSuccess(final SchemaContext result, final NetconfSessionPreferences remoteSessionCapabilities, final DOMRpcService deviceRpc) {
        messageTransformer = new NetconfMessageTransformer(result, true);

        updateTransformer(messageTransformer);
        // salFacade.onDeviceConnected has to be called before the notification handler is initialized
        salFacade.onDeviceConnected(result, remoteSessionCapabilities, deviceRpc);
        notificationHandler.onRemoteSchemaUp(messageTransformer);

        logger.info("{}: Netconf connector initialized successfully", id);
    }

    private void handleSalInitializationFailure(final Throwable t, final RemoteDeviceCommunicator<NetconfMessage> listener) {
        logger.error("{}: Initialization in sal failed, disconnecting from device", id, t);
        listener.close();
        onRemoteSessionDown();
        resetMessageTransformer();
    }

    /**
     * Set the transformer to null as is in initial state
     */
    private void resetMessageTransformer() {
        updateTransformer(null);
    }

    private void updateTransformer(final MessageTransformer<NetconfMessage> transformer) {
        messageTransformer = transformer;
    }

    private void addProvidedSourcesToSchemaRegistry(final NetconfDeviceRpc deviceRpc, final DeviceSources deviceSources) {
        final NetconfRemoteSchemaYangSourceProvider yangProvider = new NetconfRemoteSchemaYangSourceProvider(id, deviceRpc, keepaliveTimeoutMillis);
        for (final SourceIdentifier sourceId : deviceSources.getProvidedSources()) {
            sourceRegistrations.add(schemaRegistry.registerSchemaSource(yangProvider,
                    PotentialSchemaSource.create(sourceId, YangTextSchemaSource.class, PotentialSchemaSource.Costs.REMOTE_IO.getValue())));
        }
    }

    @Override
    public void onRemoteSessionDown() {
        notificationHandler.onRemoteSchemaDown();

        salFacade.onDeviceDisconnected();
        for (final SchemaSourceRegistration<? extends SchemaSourceRepresentation> sourceRegistration : sourceRegistrations) {
            sourceRegistration.close();
        }
        resetMessageTransformer();
    }

    @Override
    public void onRemoteSessionFailed(final Throwable throwable) {
        salFacade.onDeviceFailed(throwable);
    }

    @Override
    public void onNotification(final NetconfMessage notification) {
        notificationHandler.handleNotification(notification);
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

    /**
     * Schema building callable.
     */
    private static class DeviceSourcesResolver implements Callable<DeviceSources> {

        private final NetconfDeviceRpc deviceRpc;
        private final NetconfSessionPreferences remoteSessionCapabilities;
        private final RemoteDeviceId id;
        private final NetconfStateSchemas.NetconfStateSchemasResolver stateSchemasResolver;

        DeviceSourcesResolver(final NetconfDeviceRpc deviceRpc, final NetconfSessionPreferences remoteSessionCapabilities,
                                     final RemoteDeviceId id, final NetconfStateSchemas.NetconfStateSchemasResolver stateSchemasResolver) {
            this.deviceRpc = deviceRpc;
            this.remoteSessionCapabilities = remoteSessionCapabilities;
            this.id = id;
            this.stateSchemasResolver = stateSchemasResolver;
        }

        public DeviceSourcesResolver(final NetconfSessionPreferences remoteSessionCapabilities, final RemoteDeviceId id, final NetconfStateSchemas.NetconfStateSchemasResolver stateSchemasResolver, final NetconfDeviceRpc rpcForMonitoring) {
            this(rpcForMonitoring, remoteSessionCapabilities, id, stateSchemasResolver);
        }

        @Override
        public DeviceSources call() throws Exception {
            final NetconfStateSchemas availableSchemas = stateSchemasResolver.resolve(deviceRpc, remoteSessionCapabilities, id);
            logger.debug("{}: Schemas exposed by ietf-netconf-monitoring: {}", id, availableSchemas.getAvailableYangSchemasQNames());

            final Set<QName> requiredSources = Sets.newHashSet(remoteSessionCapabilities.getModuleBasedCaps());
            final Set<QName> providedSources = availableSchemas.getAvailableYangSchemasQNames();

            final Set<QName> requiredSourcesNotProvided = Sets.difference(requiredSources, providedSources);
            if (!requiredSourcesNotProvided.isEmpty()) {
                logger.warn("{}: Netconf device does not provide all yang models reported in hello message capabilities, required but not provided: {}",
                        id, requiredSourcesNotProvided);
                logger.warn("{}: Attempting to build schema context from required sources", id);
            }

            // Here all the sources reported in netconf monitoring are merged with those reported in hello.
            // It is necessary to perform this since submodules are not mentioned in hello but still required.
            // This clashes with the option of a user to specify supported yang models manually in configuration for netconf-connector
            // and as a result one is not able to fully override yang models of a device. It is only possible to add additional models.
            final Set<QName> providedSourcesNotRequired = Sets.difference(providedSources, requiredSources);
            if (!providedSourcesNotRequired.isEmpty()) {
                logger.warn("{}: Netconf device provides additional yang models not reported in hello message capabilities: {}",
                        id, providedSourcesNotRequired);
                logger.warn("{}: Adding provided but not required sources as required to prevent failures", id);
                logger.debug("{}: Netconf device reported in hello: {}", id, requiredSources);
                requiredSources.addAll(providedSourcesNotRequired);
            }

            return new DeviceSources(requiredSources, providedSources);
        }
    }

    /**
     * Contains RequiredSources - sources from capabilities.
     */
    private static final class DeviceSources {
        private final Set<QName> requiredSources;
        private final Set<QName> providedSources;

        public DeviceSources(final Set<QName> requiredSources, final Set<QName> providedSources) {
            this.requiredSources = requiredSources;
            this.providedSources = providedSources;
        }

        public Set<QName> getRequiredSourcesQName() {
            return requiredSources;
        }

        public Set<QName> getProvidedSourcesQName() {
            return providedSources;
        }

        public Collection<SourceIdentifier> getRequiredSources() {
            return Collections2.transform(requiredSources, QNAME_TO_SOURCE_ID_FUNCTION);
        }

        public Collection<SourceIdentifier> getProvidedSources() {
            return Collections2.transform(providedSources, QNAME_TO_SOURCE_ID_FUNCTION);
        }

    }

    /**
     * Schema builder that tries to build schema context from provided sources or biggest subset of it.
     */
    private final class RecursiveSchemaSetup implements Runnable {
        private final DeviceSources deviceSources;
        private final NetconfSessionPreferences remoteSessionCapabilities;
        private final RemoteDeviceCommunicator<NetconfMessage> listener;
        private final NetconfDeviceCapabilities capabilities;

        public RecursiveSchemaSetup(final DeviceSources deviceSources, final NetconfSessionPreferences remoteSessionCapabilities, final RemoteDeviceCommunicator<NetconfMessage> listener) {
            this.deviceSources = deviceSources;
            this.remoteSessionCapabilities = remoteSessionCapabilities;
            this.listener = listener;
            this.capabilities = remoteSessionCapabilities.getNetconfDeviceCapabilities();
        }

        @Override
        public void run() {
            setUpSchema(deviceSources.getRequiredSources());
        }

        /**
         * Recursively build schema context, in case of success or final failure notify device
         */
        // FIXME reimplement without recursion
        private void setUpSchema(final Collection<SourceIdentifier> requiredSources) {
            logger.trace("{}: Trying to build schema context from {}", id, requiredSources);

            // If no more sources, fail
            if(requiredSources.isEmpty()) {
                final IllegalStateException cause = new IllegalStateException(id + ": No more sources for schema context");
                handleSalInitializationFailure(cause, listener);
                salFacade.onDeviceFailed(cause);
                return;
            }

            final CheckedFuture<SchemaContext, SchemaResolutionException> schemaBuilderFuture = schemaContextFactory.createSchemaContext(requiredSources);

            final FutureCallback<SchemaContext> RecursiveSchemaBuilderCallback = new FutureCallback<SchemaContext>() {

                @Override
                public void onSuccess(final SchemaContext result) {
                    logger.debug("{}: Schema context built successfully from {}", id, requiredSources);
                    final Collection<QName> filteredQNames = Sets.difference(deviceSources.getProvidedSourcesQName(), capabilities.getUnresolvedCapabilites().keySet());
                    capabilities.addCapabilities(filteredQNames);
                    capabilities.addNonModuleBasedCapabilities(remoteSessionCapabilities.getNonModuleCaps());
                    handleSalInitializationSuccess(result, remoteSessionCapabilities, getDeviceSpecificRpc(result));
                }

                @Override
                public void onFailure(final Throwable t) {
                    // In case source missing, try without it
                    if (t instanceof MissingSchemaSourceException) {
                        final SourceIdentifier missingSource = ((MissingSchemaSourceException) t).getSourceId();
                        logger.warn("{}: Unable to build schema context, missing source {}, will reattempt without it", id, missingSource);
                        capabilities.addUnresolvedCapabilities(getQNameFromSourceIdentifiers(Sets.newHashSet(missingSource)), UnavailableCapability.FailureReason.MissingSource);
                        setUpSchema(stripMissingSource(requiredSources, missingSource));

                    // In case resolution error, try only with resolved sources
                    } else if (t instanceof SchemaResolutionException) {
                        // TODO check for infinite loop
                        final SchemaResolutionException resolutionException = (SchemaResolutionException) t;
                        final Set<SourceIdentifier> unresolvedSources = resolutionException.getUnsatisfiedImports().keySet();
                        capabilities.addUnresolvedCapabilities(getQNameFromSourceIdentifiers(unresolvedSources), UnavailableCapability.FailureReason.UnableToResolve);
                        logger.warn("{}: Unable to build schema context, unsatisfied imports {}, will reattempt with resolved only", id, resolutionException.getUnsatisfiedImports());
                        setUpSchema(resolutionException.getResolvedSources());
                    // unknown error, fail
                    } else {
                        handleSalInitializationFailure(t, listener);
                    }
                }
            };

            Futures.addCallback(schemaBuilderFuture, RecursiveSchemaBuilderCallback);
        }

        private NetconfDeviceRpc getDeviceSpecificRpc(final SchemaContext result) {
            return new NetconfDeviceRpc(result, listener, new NetconfMessageTransformer(result, true));
        }

        private Collection<SourceIdentifier> stripMissingSource(final Collection<SourceIdentifier> requiredSources, final SourceIdentifier sIdToRemove) {
            final LinkedList<SourceIdentifier> sourceIdentifiers = Lists.newLinkedList(requiredSources);
            final boolean removed = sourceIdentifiers.remove(sIdToRemove);
            Preconditions.checkState(removed, "{}: Trying to remove {} from {} failed", id, sIdToRemove, requiredSources);
            return sourceIdentifiers;
        }

        private Collection<QName> getQNameFromSourceIdentifiers(final Collection<SourceIdentifier> identifiers) {
            final Collection<QName> qNames = Collections2.transform(identifiers, new Function<SourceIdentifier, QName>() {
                @Override
                public QName apply(final SourceIdentifier sourceIdentifier) {
                    return getQNameFromSourceIdentifier(sourceIdentifier);
                }
            });

            if (qNames.isEmpty()) {
                logger.debug("Unable to map any source identfiers to a capability reported by device : " + identifiers);
            }
            return qNames;
        }

        private QName getQNameFromSourceIdentifier(final SourceIdentifier identifier) {
            // Required sources are all required and provided merged in DeviceSourcesResolver
            for (final QName qname : deviceSources.getRequiredSourcesQName()) {
                if(qname.getLocalName().equals(identifier.getName()) == false) {
                    continue;
                }

                if(identifier.getRevision().equals(SourceIdentifier.NOT_PRESENT_FORMATTED_REVISION) &&
                        qname.getRevision() == null) {
                    return qname;
                }

                if (qname.getFormattedRevision().equals(identifier.getRevision())) {
                    return qname;
                }
            }
            throw new IllegalArgumentException("Unable to map identifier to a devices reported capability: " + identifier + " Available: " + deviceSources.getRequiredSourcesQName());
        }
    }
}
