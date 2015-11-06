/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.topology.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.util.concurrent.EventExecutor;
import java.io.File;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.controller.config.threadpool.ScheduledThreadPool;
import org.opendaylight.controller.config.threadpool.ThreadPool;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfigurationBuilder;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.LoginPassword;
import org.opendaylight.controller.netconf.topology.NetconfTopology;
import org.opendaylight.controller.netconf.topology.SchemaRepositoryProvider;
import org.opendaylight.controller.netconf.topology.TopologyMountPointFacade;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.netconf.NetconfDevice;
import org.opendaylight.controller.sal.connect.netconf.NetconfStateSchemas;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCapabilities;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCommunicator;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.connect.netconf.sal.KeepaliveSalFacade;
import org.opendaylight.controller.sal.connect.netconf.sal.NetconfDeviceSalFacade;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.protocol.framework.TimedReconnectStrategy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.credentials.Credentials;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaContextFactory;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaRepository;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceFilter;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceRegistry;
import org.opendaylight.yangtools.yang.model.repo.util.FilesystemSchemaSourceCache;
import org.opendaylight.yangtools.yang.parser.repo.SharedSchemaRepository;
import org.opendaylight.yangtools.yang.parser.util.TextToASTTransformer;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfTopologyImpl implements NetconfTopology, DataTreeChangeListener<Node>, BindingAwareProvider,
        Provider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfTopologyImpl.class);

    private static final long DEFAULT_REQUEST_TIMEOUT_MILIS = 60000L;
    private static final int DEFAULT_KEEPALIVE_DELAY = 0;
    private static final boolean DEFAULT_RECONNECT_ON_CHANGED_SCHEMA = false;
    private static final int DEFAULT_MAX_CONNECTION_ATTEMPTS = 0;
    private static final int DEFAULT_BETWEEN_ATTEMPTS_TIMEOUT_MILLIS = 2000;
    private static final BigDecimal DEFAULT_SLEEP_FACTOR = new BigDecimal(1.5);

    private static FilesystemSchemaSourceCache<YangTextSchemaSource> CACHE = null;
    //keep track of already initialized repositories to avoid adding redundant listeners
    private static final Set<SchemaRepository> INITIALIZED_SCHEMA_REPOSITORIES = new HashSet<>();

    private final String topologyId;
    private final boolean listenForConfigChanges;
    private final NetconfClientDispatcher clientDispatcher;
    private final BindingAwareBroker bindingAwareBroker;
    private final Broker domBroker;
    private final EventExecutor eventExecutor;
    private final ScheduledThreadPool keepaliveExecutor;
    private final ThreadPool processingExecutor;
    private final SharedSchemaRepository sharedSchemaRepository;
    private final BundleContext bundleContext;

    private SchemaSourceRegistry schemaRegistry = null;
    private SchemaContextFactory schemaContextFactory = null;

    private DOMMountPointService mountPointService = null;
    private DataBroker dataBroker = null;
    private final HashMap<NodeId, NetconfConnectorDTO> activeConnectors = new HashMap<>();

    private ListenerRegistration<NetconfTopologyImpl> listenerRegistration = null;

    public NetconfTopologyImpl(final String topologyId, final boolean listenForConfigChanges, final NetconfClientDispatcher clientDispatcher,
            final BindingAwareBroker bindingAwareBroker, final Broker domBroker, final EventExecutor eventExecutor,
            final ScheduledThreadPool keepaliveExecutor, final ThreadPool processingExecutor,
 final SchemaRepositoryProvider sharedSchemaRepositoryProvider,
            final BundleContext bundleContext) {
        this.topologyId = topologyId;
        this.listenForConfigChanges = listenForConfigChanges;
        this.clientDispatcher = clientDispatcher;
        this.bindingAwareBroker = bindingAwareBroker;
        this.domBroker = domBroker;
        this.eventExecutor = eventExecutor;
        this.keepaliveExecutor = keepaliveExecutor;
        this.processingExecutor = processingExecutor;
        this.sharedSchemaRepository = sharedSchemaRepositoryProvider.getSharedSchemaRepository();
        this.bundleContext = Preconditions.checkNotNull(bundleContext);

        registerToSal(this, this);
    }

    private void registerToSal(final BindingAwareProvider baProvider, final Provider provider) {
        domBroker.registerProvider(provider);
        bindingAwareBroker.registerProvider(baProvider);
    }

    @Override
    public void close() throws Exception {
        // close all existing connectors, delete whole topology in datastore?
        for (final NetconfConnectorDTO connectorDTO : activeConnectors.values()) {
            connectorDTO.getCommunicator().disconnect();
        }
        activeConnectors.clear();
        if (listenerRegistration != null) {
            listenerRegistration.close();
            listenerRegistration = null;
        }
    }

    @Override
    public String getTopologyId() {
        return topologyId;
    }

    @Override
    public DataBroker getDataBroker() {
        return Preconditions.checkNotNull(dataBroker, "DataBroker not initialized yet");
    }

    @Override
    public ListenableFuture<NetconfDeviceCapabilities> connectNode(final NodeId nodeId, final Node configNode) {
        LOG.info("Connecting RemoteDevice{{}} , with config {}", nodeId, configNode);
        return setupConnection(nodeId, configNode);
    }

    @Override
    public ListenableFuture<Void> disconnectNode(final NodeId nodeId) {
        LOG.debug("Disconnecting RemoteDevice{{}}", nodeId.getValue());
        if (!activeConnectors.containsKey(nodeId)) {
            return Futures.immediateFailedFuture(new IllegalStateException(
                    "Unable to disconnect device that is not connected"));
        }

        // retrieve connection, and disconnect it
        final NetconfConnectorDTO connectionDTO = activeConnectors.remove(nodeId);
        connectionDTO.getCommunicator().close();
        connectionDTO.getFacade().close();
        return Futures.immediateFuture(null);
    }

    @Override
    public void registerConnectionStatusListener(final NodeId node,
            final RemoteDeviceHandler<NetconfSessionPreferences> listener) {
        if (activeConnectors.get(node).getFacade() instanceof TopologyMountPointFacade) {
            ((TopologyMountPointFacade) activeConnectors.get(node).getFacade()).registerConnectionStatusListener(listener);
        } else {
            LOG.warn("Unable to register a connection status listener on a regular salFacade, reconfigure for topology mountpoint facade");
        }
    }

    private ListenableFuture<NetconfDeviceCapabilities> setupConnection(final NodeId nodeId, final Node configNode) {
        final NetconfNode netconfNode = configNode.getAugmentation(NetconfNode.class);

        Preconditions.checkNotNull(netconfNode.getHost());
        Preconditions.checkNotNull(netconfNode.getPort());
        Preconditions.checkNotNull(netconfNode.isTcpOnly());

        final NetconfConnectorDTO deviceCommunicatorDTO = createDeviceCommunicator(nodeId, netconfNode);
        final NetconfDeviceCommunicator deviceCommunicator = deviceCommunicatorDTO.getCommunicator();
        final NetconfReconnectingClientConfiguration clientConfig = getClientConfig(deviceCommunicator, netconfNode);
        final ListenableFuture<NetconfDeviceCapabilities> future = deviceCommunicator.initializeRemoteConnection(
                clientDispatcher, clientConfig);

        Futures.addCallback(future, new FutureCallback<NetconfDeviceCapabilities>() {
            @Override
            public void onSuccess(final NetconfDeviceCapabilities result) {
                LOG.debug("Connector for : {} started succesfully", nodeId.getValue());
                activeConnectors.put(nodeId, deviceCommunicatorDTO);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Connector for : {} failed", nodeId.getValue());
                // remove this node from active connectors?
            }
        });

        return future;
    }

    private NetconfConnectorDTO createDeviceCommunicator(final NodeId nodeId, final NetconfNode node) {
        //setup default values since default value is not supported yet in mdsal
        // TODO remove this when mdsal starts supporting default values
        final Long defaultRequestTimeoutMillis = node.getDefaultRequestTimeoutMillis() == null ? DEFAULT_REQUEST_TIMEOUT_MILIS : node.getDefaultRequestTimeoutMillis();
        final Long keepaliveDelay = node.getKeepaliveDelay() == null ? DEFAULT_KEEPALIVE_DELAY : node.getKeepaliveDelay();
        final Boolean reconnectOnChangedSchema = node.isReconnectOnChangedSchema() == null ? DEFAULT_RECONNECT_ON_CHANGED_SCHEMA : node.isReconnectOnChangedSchema();

        final IpAddress ipAddress = node.getHost().getIpAddress();
        final InetSocketAddress address = new InetSocketAddress(ipAddress.getIpv4Address() != null ? ipAddress
                .getIpv4Address().getValue() : ipAddress.getIpv6Address().getValue(), node.getPort().getValue());
        final RemoteDeviceId remoteDeviceId = new RemoteDeviceId(nodeId.getValue(), address);

        // we might need to create a new SalFacade to maintain backwards compatibility with special case loopback connection
//        final TopologyMountPointFacade mountPointFacade = new TopologyMountPointFacade(remoteDeviceId, domBroker,
//                bindingAwareBroker, node.getDefaultRequestTimeoutMillis());
        RemoteDeviceHandler<NetconfSessionPreferences> salFacade = new NetconfDeviceSalFacade(remoteDeviceId,
                domBroker, bindingAwareBroker, bundleContext, defaultRequestTimeoutMillis);
//                new TopologyMountPointFacade(remoteDeviceId, domBroker, bindingAwareBroker, defaultRequestTimeoutMillis);

        if (keepaliveDelay > 0) {
            LOG.warn("Adding keepalive facade, for device {}", nodeId);
            salFacade = new KeepaliveSalFacade(remoteDeviceId, salFacade, keepaliveExecutor.getExecutor(), keepaliveDelay);
        }

        final NetconfDevice.SchemaResourcesDTO schemaResourcesDTO = new NetconfDevice.SchemaResourcesDTO(
                schemaRegistry, schemaContextFactory, new NetconfStateSchemas.NetconfStateSchemasResolverImpl());

        final NetconfDevice device = new NetconfDevice(schemaResourcesDTO, remoteDeviceId, salFacade,
                processingExecutor.getExecutor(), reconnectOnChangedSchema);

        return new NetconfConnectorDTO(new NetconfDeviceCommunicator(remoteDeviceId, device), salFacade);
    }

    public NetconfReconnectingClientConfiguration getClientConfig(final NetconfDeviceCommunicator listener,
            final NetconfNode node) {

      //setup default values since default value is not supported yet in mdsal
        // TODO remove this when mdsal starts supporting default values
        final long clientConnectionTimeoutMillis = node.getDefaultRequestTimeoutMillis() == null ? DEFAULT_REQUEST_TIMEOUT_MILIS : node.getDefaultRequestTimeoutMillis();
        final long maxConnectionAttempts = node.getMaxConnectionAttempts() == null ? DEFAULT_MAX_CONNECTION_ATTEMPTS : node.getMaxConnectionAttempts();
        final int betweenAttemptsTimeoutMillis = node.getBetweenAttemptsTimeoutMillis() == null ? DEFAULT_BETWEEN_ATTEMPTS_TIMEOUT_MILLIS : node.getBetweenAttemptsTimeoutMillis();
        final BigDecimal sleepFactor = node.getSleepFactor() == null ? DEFAULT_SLEEP_FACTOR : node.getSleepFactor();

        final InetSocketAddress socketAddress = getSocketAddress(node.getHost(), node.getPort().getValue());

        final ReconnectStrategyFactory sf = new TimedReconnectStrategyFactory(eventExecutor, maxConnectionAttempts,
                betweenAttemptsTimeoutMillis, sleepFactor);
        final ReconnectStrategy strategy = sf.createReconnectStrategy();

        final AuthenticationHandler authHandler;
        final Credentials credentials = node.getCredentials();
        if (credentials instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.credentials.credentials.LoginPassword) {
            authHandler = new LoginPassword(
                    ((org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.credentials.credentials.LoginPassword) credentials)
                            .getUsername(),
                    ((org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.credentials.credentials.LoginPassword) credentials)
                            .getPassword());
        } else {
            throw new IllegalStateException("Only login/password authentification is supported");
        }

        return NetconfReconnectingClientConfigurationBuilder
                .create()
                .withAddress(socketAddress)
                .withConnectionTimeoutMillis(clientConnectionTimeoutMillis)
                .withReconnectStrategy(strategy)
                .withAuthHandler(authHandler)
                .withProtocol(
                        node.isTcpOnly() ? NetconfClientConfiguration.NetconfClientProtocol.TCP
                                : NetconfClientConfiguration.NetconfClientProtocol.SSH).withConnectStrategyFactory(sf)
                .withSessionListener(listener).build();
    }

    @Override
    public void onSessionInitiated(final ProviderSession session) {
        mountPointService = session.getService(DOMMountPointService.class);
        if (listenForConfigChanges) {
            LOG.warn("Registering datastore listener");
            listenerRegistration = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(
                    LogicalDatastoreType.CONFIGURATION, createTopologyId(topologyId).child(Node.class)), this);
        }
    }

    @Override
    public void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<Node>> collection) {
        for (final DataTreeModification<Node> change : collection) {
            final DataObjectModification<Node> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
            case SUBTREE_MODIFIED:
                LOG.debug("Config for node {} updated", getNodeId(rootNode.getIdentifier()));
                disconnectNode(getNodeId(rootNode.getIdentifier()));
                connectNode(getNodeId(rootNode.getIdentifier()), rootNode.getDataAfter());
                break;
            case WRITE:
                LOG.debug("Config for node {} created", getNodeId(rootNode.getIdentifier()));
                if (activeConnectors.containsKey(getNodeId(rootNode.getIdentifier()))) {
                    LOG.warn("RemoteDevice{{}} was already configured, reconfiguring..");
                    disconnectNode(getNodeId(rootNode.getIdentifier()));
                }
                connectNode(getNodeId(rootNode.getIdentifier()), rootNode.getDataAfter());
                break;
            case DELETE:
                LOG.debug("Config for node {} deleted", getNodeId(rootNode.getIdentifier()));
                disconnectNode(getNodeId(rootNode.getIdentifier()));
                break;
            }
        }
    }

    private void initFilesystemSchemaSourceCache(final SharedSchemaRepository repository) {
        LOG.warn("Schema repository used: {}", repository.getIdentifier());
        if (CACHE == null) {
            CACHE = new FilesystemSchemaSourceCache<>(repository, YangTextSchemaSource.class, new File("cache/schema"));
        }
        if (!INITIALIZED_SCHEMA_REPOSITORIES.contains(repository)) {
            repository.registerSchemaSourceListener(CACHE);
            repository.registerSchemaSourceListener(TextToASTTransformer.create(repository, repository));
            INITIALIZED_SCHEMA_REPOSITORIES.add(repository);
        }
        setSchemaRegistry(repository);
        setSchemaContextFactory(repository.createSchemaContextFactory(SchemaSourceFilter.ALWAYS_ACCEPT));
    }

    public void setSchemaRegistry(final SchemaSourceRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    public void setSchemaContextFactory(final SchemaContextFactory schemaContextFactory) {
        this.schemaContextFactory = schemaContextFactory;
    }

    //TODO this needs to be an util method, since netconf clustering uses this aswell
    /**
     * Determines the Netconf Node Node ID, given the node's instance
     * identifier.
     *
     * @param pathArgument Node's path arument
     * @return NodeId for the node
     */
    private NodeId getNodeId(final PathArgument pathArgument) {
        if (pathArgument instanceof InstanceIdentifier.IdentifiableItem<?, ?>) {

            final Identifier key = ((InstanceIdentifier.IdentifiableItem) pathArgument).getKey();
            if (key instanceof NodeKey) {
                return ((NodeKey) key).getNodeId();
            }
        }
        throw new IllegalStateException("Unable to create NodeId from: " + pathArgument);
    }

    private static InstanceIdentifier<Topology> createTopologyId(final String topologyId) {
        final InstanceIdentifier<NetworkTopology> networkTopology = InstanceIdentifier.create(NetworkTopology.class);
        return networkTopology.child(Topology.class, new TopologyKey(new TopologyId(topologyId)));
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public void onSessionInitiated(final ProviderContext session) {
        dataBroker = session.getSALService(DataBroker.class);
    }

    private static final class NetconfConnectorDTO {

        private final NetconfDeviceCommunicator communicator;
        private final RemoteDeviceHandler<NetconfSessionPreferences> facade;

        private NetconfConnectorDTO(final NetconfDeviceCommunicator communicator,
                final RemoteDeviceHandler<NetconfSessionPreferences> facade) {
            this.communicator = communicator;
            this.facade = facade;
        }

        public NetconfDeviceCommunicator getCommunicator() {
            return communicator;
        }

        public RemoteDeviceHandler<NetconfSessionPreferences> getFacade() {
            return facade;
        }
    }

    private static final class TimedReconnectStrategyFactory implements ReconnectStrategyFactory {
        private final Long connectionAttempts;
        private final EventExecutor executor;
        private final double sleepFactor;
        private final int minSleep;

        TimedReconnectStrategyFactory(final EventExecutor executor, final Long maxConnectionAttempts,
                final int minSleep, final BigDecimal sleepFactor) {
            if (maxConnectionAttempts != null && maxConnectionAttempts > 0) {
                connectionAttempts = maxConnectionAttempts;
            } else {
                connectionAttempts = null;
            }

            this.sleepFactor = sleepFactor.doubleValue();
            this.executor = executor;
            this.minSleep = minSleep;
        }

        @Override
        public ReconnectStrategy createReconnectStrategy() {
            final Long maxSleep = null;
            final Long deadline = null;

            return new TimedReconnectStrategy(executor, minSleep, minSleep, sleepFactor, maxSleep, connectionAttempts,
                    deadline);
        }
    }

    private InetSocketAddress getSocketAddress(final Host host, final int port) {
        if (host.getDomainName() != null) {
            return new InetSocketAddress(host.getDomainName().getValue(), port);
        } else {
            final IpAddress ipAddress = host.getIpAddress();
            final String ip = ipAddress.getIpv4Address() != null ? ipAddress.getIpv4Address().getValue() : ipAddress
                    .getIpv6Address().getValue();
            return new InetSocketAddress(ip, port);
        }
    }
}

