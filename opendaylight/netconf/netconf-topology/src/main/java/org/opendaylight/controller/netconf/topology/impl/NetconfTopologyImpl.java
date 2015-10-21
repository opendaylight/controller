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
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import org.opendaylight.controller.config.threadpool.ScheduledThreadPool;
import org.opendaylight.controller.config.threadpool.ThreadPool;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaContextFactory;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfTopologyImpl implements NetconfTopology, BindingAwareProvider, Provider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfTopologyImpl.class);

    private final String topologyId;
    private final NetconfClientDispatcher clientDispatcher;
    private final BindingAwareBroker bindingAwareBroker;
    private final Broker domBroker;
    private final EventExecutor eventExecutor;
    private final ScheduledThreadPool keepaliveExecutor;
    private final ThreadPool processingExecutor;
    private final SchemaRepositoryProvider sharedSchemaRepository;

    private final SchemaSourceRegistry schemaSourceRegistry = null;
    private final SchemaContextFactory schemaContextFactory = null;

    private DOMMountPointService mountPointService = null;
    private DataBroker dataBroker = null;
    private final HashMap<NodeId, NetconfConnectorDTO> activeConnectors = new HashMap<>();

    public NetconfTopologyImpl(final String topologyId, final NetconfClientDispatcher clientDispatcher,
            final BindingAwareBroker bindingAwareBroker, final Broker domBroker, final EventExecutor eventExecutor,
            final ScheduledThreadPool keepaliveExecutor, final ThreadPool processingExecutor,
            final SchemaRepositoryProvider sharedSchemaRepository) {
        this.topologyId = topologyId;
        this.clientDispatcher = clientDispatcher;
        this.bindingAwareBroker = bindingAwareBroker;
        this.domBroker = domBroker;
        this.eventExecutor = eventExecutor;
        this.keepaliveExecutor = keepaliveExecutor;
        this.processingExecutor = processingExecutor;
        this.sharedSchemaRepository = sharedSchemaRepository;

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
        return setupConnection(nodeId, configNode);
    }

    @Override
    public ListenableFuture<Void> disconnectNode(final NodeId nodeId) {
        if (!activeConnectors.containsKey(nodeId)) {
            return Futures.immediateFailedFuture(new IllegalStateException(
                    "Unable to disconnect device that is not connected"));
        }

        // retrieve connection, and disconnect it
        activeConnectors.remove(nodeId).getCommunicator().disconnect();
        return Futures.immediateFuture(null);
    }

    @Override
    public void registerConnectionStatusListener(final NodeId node,
            final RemoteDeviceHandler<NetconfSessionPreferences> listener) {
        activeConnectors.get(node).getMountPointFacade().registerConnectionStatusListener(listener);
    }

    private ListenableFuture<NetconfDeviceCapabilities> setupConnection(final NodeId nodeId, final Node configNode) {
        final NetconfNode netconfNode = configNode.getAugmentation(NetconfNode.class);

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
        final IpAddress ipAddress = node.getHost().getIpAddress();
        final InetSocketAddress address = new InetSocketAddress(ipAddress.getIpv4Address() != null ? ipAddress
                .getIpv4Address().getValue() : ipAddress.getIpv6Address().getValue(), node.getPort().getValue());
        final RemoteDeviceId remoteDeviceId = new RemoteDeviceId(nodeId.getValue(), address);

        // we might need to create a new SalFacade to maintain backwards compatibility with special case loopback connection
        final TopologyMountPointFacade mountPointFacade = new TopologyMountPointFacade(remoteDeviceId, domBroker,
                bindingAwareBroker, node.getDefaultRequestTimeoutMillis());
        RemoteDeviceHandler<NetconfSessionPreferences> salFacade = mountPointFacade;
        if (node.getKeepaliveDelay() > 0) {
            salFacade = new KeepaliveSalFacade(remoteDeviceId, mountPointFacade, keepaliveExecutor.getExecutor(),
                    node.getKeepaliveDelay());
        }

        final NetconfDevice.SchemaResourcesDTO schemaResourcesDTO = new NetconfDevice.SchemaResourcesDTO(
                schemaSourceRegistry, schemaContextFactory, new NetconfStateSchemas.NetconfStateSchemasResolverImpl());

        final NetconfDevice device = new NetconfDevice(schemaResourcesDTO, remoteDeviceId, salFacade,
                processingExecutor.getExecutor(), node.isReconnectOnChangedSchema());

        return new NetconfConnectorDTO(new NetconfDeviceCommunicator(remoteDeviceId, device), mountPointFacade);
    }

    public NetconfReconnectingClientConfiguration getClientConfig(final NetconfDeviceCommunicator listener,
            final NetconfNode node) {
        final InetSocketAddress socketAddress = getSocketAddress(node.getHost(), node.getPort().getValue());
        final long clientConnectionTimeoutMillis = node.getDefaultRequestTimeoutMillis();

        final ReconnectStrategyFactory sf = new TimedReconnectStrategyFactory(eventExecutor,
                node.getMaxConnectionAttempts(), node.getBetweenAttemptsTimeoutMillis(), node.getSleepFactor());
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
        private final TopologyMountPointFacade mountPointFacade;

        private NetconfConnectorDTO(final NetconfDeviceCommunicator communicator,
                final TopologyMountPointFacade mountPointFacade) {
            this.communicator = communicator;
            this.mountPointFacade = mountPointFacade;
        }

        public NetconfDeviceCommunicator getCommunicator() {
            return communicator;
        }

        public TopologyMountPointFacade getMountPointFacade() {
            return mountPointFacade;
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

