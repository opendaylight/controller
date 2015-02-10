/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf;

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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.sal.connect.api.MessageTransformer;
import org.opendaylight.controller.sal.connect.api.RemoteDevice;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceCommunicator;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCapabilities;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.connect.netconf.sal.NetconfDeviceRpc;
import org.opendaylight.controller.sal.connect.netconf.schema.NetconfRemoteSchemaYangSourceProvider;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.fields.unavailable.capabilities.UnavailableCapability.FailureReason;
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
public final class NetconfDevice implements RemoteDevice<NetconfSessionPreferences, NetconfMessage> {

    private static final Logger logger = LoggerFactory.getLogger(NetconfDevice.class);

    public static final Function<QName, SourceIdentifier> QNAME_TO_SOURCE_ID_FUNCTION = new Function<QName, SourceIdentifier>() {
        @Override
        public SourceIdentifier apply(final QName input) {
            return new SourceIdentifier(input.getLocalName(), Optional.fromNullable(input.getFormattedRevision()));
        }
    };

    private final RemoteDeviceId id;

    private final SchemaContextFactory schemaContextFactory;
    private final RemoteDeviceHandler<NetconfSessionPreferences> salFacade;
    private final ListeningExecutorService processingExecutor;
    private final SchemaSourceRegistry schemaRegistry;
    private final MessageTransformer<NetconfMessage> messageTransformer;
    private final NetconfStateSchemas.NetconfStateSchemasResolver stateSchemasResolver;
    private final NotificationHandler notificationHandler;
    private final List<SchemaSourceRegistration<? extends SchemaSourceRepresentation>> sourceRegistrations = Lists.newArrayList();

    public NetconfDevice(final SchemaResourcesDTO schemaResourcesDTO, final RemoteDeviceId id, final RemoteDeviceHandler<NetconfSessionPreferences> salFacade,
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
    public void onRemoteSessionUp(final NetconfSessionPreferences remoteSessionCapabilities,
                                  final RemoteDeviceCommunicator<NetconfMessage> listener) {
        // SchemaContext setup has to be performed in a dedicated thread since
        // we are in a netty thread in this method
        // Yang models are being downloaded in this method and it would cause a
        // deadlock if we used the netty thread
        // http://netty.io/wiki/thread-model.html
        logger.debug("{}: Session to remote device established with {}", id, remoteSessionCapabilities);

        final NetconfDeviceRpc deviceRpc = setUpDeviceRpc(listener);

        final DeviceSourcesResolver task = new DeviceSourcesResolver(deviceRpc, remoteSessionCapabilities, id, stateSchemasResolver);
        final ListenableFuture<DeviceSources> sourceResolverFuture = processingExecutor.submit(task);

        final FutureCallback<DeviceSources> resolvedSourceCallback = new FutureCallback<DeviceSources>() {
            @Override
            public void onSuccess(final DeviceSources result) {
                addProvidedSourcesToSchemaRegistry(deviceRpc, result);
                setUpSchema(result);
            }

            private void setUpSchema(final DeviceSources result) {
                processingExecutor.submit(new RecursiveSchemaSetup(result, remoteSessionCapabilities, deviceRpc, listener));
            }

            @Override
            public void onFailure(final Throwable t) {
                logger.warn("{}: Unexpected error resolving device sources: {}", id, t);
                handleSalInitializationFailure(t, listener);
            }
        };

        Futures.addCallback(sourceResolverFuture, resolvedSourceCallback);

    }

    private void handleSalInitializationSuccess(final SchemaContext result, final NetconfSessionPreferences remoteSessionCapabilities, final NetconfDeviceRpc deviceRpc) {
        updateMessageTransformer(result);
        salFacade.onDeviceConnected(result, remoteSessionCapabilities, deviceRpc);
        notificationHandler.onRemoteSchemaUp();

        logger.debug("{}: Initialization in sal successful", id);
        logger.info("{}: Netconf connector initialized successfully", id);
    }

    private void handleSalInitializationFailure(final Throwable t, final RemoteDeviceCommunicator<NetconfMessage> listener) {
        logger.error("{}: Initialization in sal failed, disconnecting from device", id, t);
        listener.close();
        onRemoteSessionDown();
        resetMessageTransformer();
    }

    /**
     * Set the schema context inside transformer to null as is in initial state
     */
    private void resetMessageTransformer() {
        updateMessageTransformer(null);
    }

    /**
     * Update initial message transformer to use retrieved schema
     * @param currentSchemaContext
     */
    private void updateMessageTransformer(final SchemaContext currentSchemaContext) {
        messageTransformer.onGlobalContextUpdated(currentSchemaContext);
    }

    private void addProvidedSourcesToSchemaRegistry(final NetconfDeviceRpc deviceRpc, final DeviceSources deviceSources) {
        final NetconfRemoteSchemaYangSourceProvider yangProvider = new NetconfRemoteSchemaYangSourceProvider(id, deviceRpc);
        for (final SourceIdentifier sourceId : deviceSources.getProvidedSources()) {
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
        resetMessageTransformer();
    }

    @Override
    public void onRemoteSessionFailed(Throwable throwable) {
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

        public DeviceSourcesResolver(final NetconfDeviceRpc deviceRpc, final NetconfSessionPreferences remoteSessionCapabilities, final RemoteDeviceId id, final NetconfStateSchemas.NetconfStateSchemasResolver stateSchemasResolver) {
            this.deviceRpc = deviceRpc;
            this.remoteSessionCapabilities = remoteSessionCapabilities;
            this.id = id;
            this.stateSchemasResolver = stateSchemasResolver;
        }

        @Override
        public DeviceSources call() throws Exception {

            final Set<SourceIdentifier> requiredSources = Sets.newHashSet(Collections2.transform(
                    remoteSessionCapabilities.getModuleBasedCaps(), QNAME_TO_SOURCE_ID_FUNCTION));

            // If monitoring is not supported, we will still attempt to create schema, sources might be already provided
            final NetconfStateSchemas availableSchemas = stateSchemasResolver.resolve(deviceRpc, remoteSessionCapabilities, id);
            logger.debug("{}: Schemas exposed by ietf-netconf-monitoring: {}", id, availableSchemas.getAvailableYangSchemasQNames());

            final Set<SourceIdentifier> providedSources = Sets.newHashSet(Collections2.transform(
                    availableSchemas.getAvailableYangSchemasQNames(), QNAME_TO_SOURCE_ID_FUNCTION));

            final Set<SourceIdentifier> requiredSourcesNotProvided = Sets.difference(requiredSources, providedSources);

            if (!requiredSourcesNotProvided.isEmpty()) {
                logger.warn("{}: Netconf device does not provide all yang models reported in hello message capabilities, required but not provided: {}",
                        id, requiredSourcesNotProvided);
                logger.warn("{}: Attempting to build schema context from required sources", id);
            }


            // TODO should we perform this ? We have a mechanism to fix initialization of devices not reporting or required modules in hello
            // That is overriding capabilities in configuration using attribute yang-module-capabilities
            // This is more user friendly even though it clashes with attribute yang-module-capabilities
            // Some devices do not report all required models in hello message, but provide them
            final Set<SourceIdentifier> providedSourcesNotRequired = Sets.difference(providedSources, requiredSources);
            if (!providedSourcesNotRequired.isEmpty()) {
                logger.warn("{}: Netconf device provides additional yang models not reported in hello message capabilities: {}",
                        id, providedSourcesNotRequired);
                logger.warn("{}: Adding provided but not required sources as required to prevent failures", id);
                requiredSources.addAll(providedSourcesNotRequired);
            }

            return new DeviceSources(requiredSources, providedSources);
        }
    }

    /**
     * Contains RequiredSources - sources from capabilities.
     *
     */
    private static final class DeviceSources {
        private final Collection<SourceIdentifier> requiredSources;
        private final Collection<SourceIdentifier> providedSources;

        public DeviceSources(final Collection<SourceIdentifier> requiredSources, final Collection<SourceIdentifier> providedSources) {
            this.requiredSources = requiredSources;
            this.providedSources = providedSources;
        }

        public Collection<SourceIdentifier> getRequiredSources() {
            return requiredSources;
        }

        public Collection<SourceIdentifier> getProvidedSources() {
            return providedSources;
        }

    }

    /**
     * Schema builder that tries to build schema context from provided sources or biggest subset of it.
     */
    private final class RecursiveSchemaSetup implements Runnable {
        private final DeviceSources deviceSources;
        private final NetconfSessionPreferences remoteSessionCapabilities;
        private final NetconfDeviceRpc deviceRpc;
        private final RemoteDeviceCommunicator<NetconfMessage> listener;
        private NetconfDeviceCapabilities capabilities;

        public RecursiveSchemaSetup(final DeviceSources deviceSources, final NetconfSessionPreferences remoteSessionCapabilities, final NetconfDeviceRpc deviceRpc, final RemoteDeviceCommunicator<NetconfMessage> listener) {
            this.deviceSources = deviceSources;
            this.remoteSessionCapabilities = remoteSessionCapabilities;
            this.deviceRpc = deviceRpc;
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
                handleSalInitializationFailure(new IllegalStateException(id + ": No more sources for schema context"), listener);
                return;
            }

            final CheckedFuture<SchemaContext, SchemaResolutionException> schemaBuilderFuture = schemaContextFactory.createSchemaContext(requiredSources);

            final FutureCallback<SchemaContext> RecursiveSchemaBuilderCallback = new FutureCallback<SchemaContext>() {

                @Override
                public void onSuccess(final SchemaContext result) {
                    logger.debug("{}: Schema context built successfully from {}", id, requiredSources);
                    Collection<QName> filteredQNames = Sets.difference(remoteSessionCapabilities.getModuleBasedCaps(), capabilities.getUnresolvedCapabilites().keySet());
                    capabilities.addCapabilities(filteredQNames);
                    capabilities.addNonModuleBasedCapabilities(remoteSessionCapabilities.getNonModuleCaps());
                    handleSalInitializationSuccess(result, remoteSessionCapabilities, deviceRpc);
                }

                @Override
                public void onFailure(final Throwable t) {
                    // In case source missing, try without it
                    if (t instanceof MissingSchemaSourceException) {
                        final SourceIdentifier missingSource = ((MissingSchemaSourceException) t).getSourceId();
                        logger.warn("{}: Unable to build schema context, missing source {}, will reattempt without it", id, missingSource);
                        capabilities.addUnresolvedCapabilities(getQNameFromSourceIdentifiers(Sets.newHashSet(missingSource)), FailureReason.MissingSource);
                        setUpSchema(stripMissingSource(requiredSources, missingSource));

                    // In case resolution error, try only with resolved sources
                    } else if (t instanceof SchemaResolutionException) {
                        // TODO check for infinite loop
                        final SchemaResolutionException resolutionException = (SchemaResolutionException) t;
                        final Set<SourceIdentifier> unresolvedSources = resolutionException.getUnsatisfiedImports().keySet();
                        capabilities.addUnresolvedCapabilities(getQNameFromSourceIdentifiers(unresolvedSources), FailureReason.UnableToResolve);
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

        private Collection<SourceIdentifier> stripMissingSource(final Collection<SourceIdentifier> requiredSources, final SourceIdentifier sIdToRemove) {
            final LinkedList<SourceIdentifier> sourceIdentifiers = Lists.newLinkedList(requiredSources);
            final boolean removed = sourceIdentifiers.remove(sIdToRemove);
            Preconditions.checkState(removed, "{}: Trying to remove {} from {} failed", id, sIdToRemove, requiredSources);
            return sourceIdentifiers;
        }

        private Collection<QName> getQNameFromSourceIdentifiers(Collection<SourceIdentifier> identifiers) {
            Collection<QName> qNames = new HashSet<>();
            for (SourceIdentifier source : identifiers) {
                Optional<QName> qname = getQNameFromSourceIdentifier(source);
                if (qname.isPresent()) {
                    qNames.add(qname.get());
                }
            }
            if (qNames.isEmpty()) {
                logger.debug("Unable to map any source identfiers to a capability reported by device : " + identifiers);
            }
            return qNames;
        }

        private Optional<QName> getQNameFromSourceIdentifier(SourceIdentifier identifier) {
            for (QName qname : remoteSessionCapabilities.getModuleBasedCaps()) {
                if (qname.getLocalName().equals(identifier.getName())
                        && qname.getFormattedRevision().equals(identifier.getRevision())) {
                    return Optional.of(qname);
                }
            }
            throw new IllegalArgumentException("Unable to map identifier to a devices reported capability: " + identifier);
        }
    }
}
