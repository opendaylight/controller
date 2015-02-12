/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.connector.netconf;

import static org.opendaylight.controller.config.api.JmxAttributeValidationException.checkCondition;
import static org.opendaylight.controller.config.api.JmxAttributeValidationException.checkNotNull;

import com.google.common.base.Optional;
import io.netty.util.concurrent.EventExecutor;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfigurationBuilder;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.LoginPassword;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.netconf.NetconfDevice;
import org.opendaylight.controller.sal.connect.netconf.NetconfStateSchemas;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCommunicator;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.connect.netconf.sal.NetconfDeviceSalFacade;
import org.opendaylight.controller.sal.connect.netconf.schema.mapping.NetconfMessageTransformer;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.protocol.framework.TimedReconnectStrategy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaContextFactory;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceRegistry;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public final class NetconfConnectorModule extends org.opendaylight.controller.config.yang.md.sal.connector.netconf.AbstractNetconfConnectorModule
{
    private static final Logger logger = LoggerFactory.getLogger(NetconfConnectorModule.class);

    private BundleContext bundleContext;
    private Optional<NetconfSessionPreferences> userCapabilities;
    private SchemaSourceRegistry schemaRegistry;
    private SchemaContextFactory schemaContextFactory;

    public NetconfConnectorModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfConnectorModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final NetconfConnectorModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation() {
        checkNotNull(getAddress(), addressJmxAttribute);
        checkCondition(isHostAddressPresent(getAddress()), "Host address not present in " + getAddress(), addressJmxAttribute);
        checkNotNull(getPort(), portJmxAttribute);
        checkNotNull(getDomRegistry(), portJmxAttribute);
        checkNotNull(getDomRegistry(), domRegistryJmxAttribute);

        checkNotNull(getConnectionTimeoutMillis(), connectionTimeoutMillisJmxAttribute);
        checkCondition(getConnectionTimeoutMillis() > 0, "must be > 0", connectionTimeoutMillisJmxAttribute);

        checkNotNull(getBetweenAttemptsTimeoutMillis(), betweenAttemptsTimeoutMillisJmxAttribute);
        checkCondition(getBetweenAttemptsTimeoutMillis() > 0, "must be > 0", betweenAttemptsTimeoutMillisJmxAttribute);

        checkNotNull(getClientDispatcher(), clientDispatcherJmxAttribute);
        checkNotNull(getBindingRegistry(), bindingRegistryJmxAttribute);
        checkNotNull(getProcessingExecutor(), processingExecutorJmxAttribute);

        // Check username + password in case of ssh
        if(getTcpOnly() == false) {
            checkNotNull(getUsername(), usernameJmxAttribute);
            checkNotNull(getPassword(), passwordJmxAttribute);
        }

        userCapabilities = getUserCapabilities();
    }

    private boolean isHostAddressPresent(final Host address) {
        return address.getDomainName() != null ||
               address.getIpAddress() != null && (address.getIpAddress().getIpv4Address() != null || address.getIpAddress().getIpv6Address() != null);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final RemoteDeviceId id = new RemoteDeviceId(getIdentifier(), getSocketAddress());

        final ExecutorService globalProcessingExecutor = getProcessingExecutorDependency().getExecutor();

        final Broker domBroker = getDomRegistryDependency();
        final BindingAwareBroker bindingBroker = getBindingRegistryDependency();

        final RemoteDeviceHandler<NetconfSessionPreferences> salFacade
                = new NetconfDeviceSalFacade(id, domBroker, bindingBroker, bundleContext, globalProcessingExecutor);

        final NetconfDevice.SchemaResourcesDTO schemaResourcesDTO =
                new NetconfDevice.SchemaResourcesDTO(schemaRegistry, schemaContextFactory, new NetconfStateSchemas.NetconfStateSchemasResolverImpl());

        final NetconfDevice device =
                new NetconfDevice(schemaResourcesDTO, id, salFacade, globalProcessingExecutor, new NetconfMessageTransformer(), true);

        final NetconfDeviceCommunicator listener = userCapabilities.isPresent() ?
                new NetconfDeviceCommunicator(id, device, userCapabilities.get()) : new NetconfDeviceCommunicator(id, device);

        final NetconfReconnectingClientConfiguration clientConfig = getClientConfig(listener);
        final NetconfClientDispatcher dispatcher = getClientDispatcherDependency();

        listener.initializeRemoteConnection(dispatcher, clientConfig);

        return new SalConnectorCloseable(listener, salFacade);
    }

    private Optional<NetconfSessionPreferences> getUserCapabilities() {
        if(getYangModuleCapabilities() == null) {
            return Optional.absent();
        }

        final List<String> capabilities = getYangModuleCapabilities().getCapability();
        if(capabilities == null || capabilities.isEmpty()) {
            return Optional.absent();
        }

        final NetconfSessionPreferences parsedOverrideCapabilities = NetconfSessionPreferences.fromStrings(capabilities);
        JmxAttributeValidationException.checkCondition(
                parsedOverrideCapabilities.getNonModuleCaps().isEmpty(),
                "Capabilities to override can only contain module based capabilities, non-module capabilities will be retrieved from the device," +
                        " configured non-module capabilities: " + parsedOverrideCapabilities.getNonModuleCaps(),
                yangModuleCapabilitiesJmxAttribute);

        return Optional.of(parsedOverrideCapabilities);
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public NetconfReconnectingClientConfiguration getClientConfig(final NetconfDeviceCommunicator listener) {
        final InetSocketAddress socketAddress = getSocketAddress();
        final long clientConnectionTimeoutMillis = getConnectionTimeoutMillis();

        final ReconnectStrategyFactory sf = new TimedReconnectStrategyFactory(
            getEventExecutorDependency(), getMaxConnectionAttempts(), getBetweenAttemptsTimeoutMillis(), getSleepFactor());
        final ReconnectStrategy strategy = sf.createReconnectStrategy();

        return NetconfReconnectingClientConfigurationBuilder.create()
        .withAddress(socketAddress)
        .withConnectionTimeoutMillis(clientConnectionTimeoutMillis)
        .withReconnectStrategy(strategy)
        .withAuthHandler(new LoginPassword(getUsername(),getPassword()))
        .withProtocol(getTcpOnly() ?
                NetconfClientConfiguration.NetconfClientProtocol.TCP :
                NetconfClientConfiguration.NetconfClientProtocol.SSH)
        .withConnectStrategyFactory(sf)
        .withSessionListener(listener)
        .build();
    }

    private static final class SalConnectorCloseable implements AutoCloseable {
        private final RemoteDeviceHandler<NetconfSessionPreferences> salFacade;
        private final NetconfDeviceCommunicator listener;

        public SalConnectorCloseable(final NetconfDeviceCommunicator listener,
                                     final RemoteDeviceHandler<NetconfSessionPreferences> salFacade) {
            this.listener = listener;
            this.salFacade = salFacade;
        }

        @Override
        public void close() {
            listener.close();
            salFacade.close();
        }
    }

    private static final class TimedReconnectStrategyFactory implements ReconnectStrategyFactory {
        private final Long connectionAttempts;
        private final EventExecutor executor;
        private final double sleepFactor;
        private final int minSleep;

        TimedReconnectStrategyFactory(final EventExecutor executor, final Long maxConnectionAttempts, final int minSleep, final BigDecimal sleepFactor) {
            if (maxConnectionAttempts != null && maxConnectionAttempts > 0) {
                connectionAttempts = maxConnectionAttempts;
            } else {
                logger.trace("Setting {} on {} to infinity", maxConnectionAttemptsJmxAttribute, this);
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

            return new TimedReconnectStrategy(executor, minSleep,
                    minSleep, sleepFactor, maxSleep, connectionAttempts, deadline);
        }
    }

    private InetSocketAddress getSocketAddress() {
        if(getAddress().getDomainName() != null) {
            return new InetSocketAddress(getAddress().getDomainName().getValue(), getPort().getValue());
        } else {
            final IpAddress ipAddress = getAddress().getIpAddress();
            final String ip = ipAddress.getIpv4Address() != null ? ipAddress.getIpv4Address().getValue() : ipAddress.getIpv6Address().getValue();
            return new InetSocketAddress(ip, getPort().getValue());
        }
    }

    public void setSchemaRegistry(final SchemaSourceRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    public void setSchemaContextFactory(final SchemaContextFactory schemaContextFactory) {
        this.schemaContextFactory = schemaContextFactory;
    }
}
