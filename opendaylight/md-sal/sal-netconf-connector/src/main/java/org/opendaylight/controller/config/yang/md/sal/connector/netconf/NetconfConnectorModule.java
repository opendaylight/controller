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

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcherImpl;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.controller.netconf.util.handler.ssh.authentication.LoginPassword;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceSalFacade;
import org.opendaylight.controller.sal.connect.netconf.NetconfDevice;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCommunicator;
import org.opendaylight.controller.sal.connect.netconf.sal.NetconfDeviceSalFacade;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.TimedReconnectStrategy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.util.repo.AbstractCachingSchemaSourceProvider;
import org.opendaylight.yangtools.yang.model.util.repo.FilesystemSchemaCachingProvider;
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProvider;
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProviders;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InetAddresses;

import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 *
 */
public final class NetconfConnectorModule extends org.opendaylight.controller.config.yang.md.sal.connector.netconf.AbstractNetconfConnectorModule
{
    private static final Logger logger = LoggerFactory.getLogger(NetconfConnectorModule.class);

    private static ExecutorService GLOBAL_PROCESSING_EXECUTOR = null;
    private static AbstractCachingSchemaSourceProvider<String, InputStream> GLOBAL_NETCONF_SOURCE_PROVIDER = null;
    private BundleContext bundleContext;

    public NetconfConnectorModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfConnectorModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final NetconfConnectorModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation() {
        checkNotNull(getAddress(), addressJmxAttribute);
        //checkState(getAddress().getIpv4Address() != null || getAddress().getIpv6Address() != null,"Address must be set.");
        checkNotNull(getPort(), portJmxAttribute);
        checkNotNull(getDomRegistry(), portJmxAttribute);
        checkNotNull(getDomRegistry(), domRegistryJmxAttribute);

        checkNotNull(getConnectionTimeoutMillis(), connectionTimeoutMillisJmxAttribute);
        checkCondition(getConnectionTimeoutMillis() > 0, "must be > 0", connectionTimeoutMillisJmxAttribute);

        checkNotNull(getBetweenAttemptsTimeoutMillis(), betweenAttemptsTimeoutMillisJmxAttribute);
        checkCondition(getBetweenAttemptsTimeoutMillis() > 0, "must be > 0", betweenAttemptsTimeoutMillisJmxAttribute);

        // FIXME BUG-944 remove backwards compatibility
        if(getClientDispatcher() == null) {
            checkCondition(getBossThreadGroup() != null, "Client dispatcher was not set, thread groups have to be set instead", bossThreadGroupJmxAttribute);
            checkCondition(getWorkerThreadGroup() != null, "Client dispatcher was not set, thread groups have to be set instead", workerThreadGroupJmxAttribute);
        }

        // Check username + password in case of ssh
        if(getTcpOnly() == false) {
            checkNotNull(getUsername(), usernameJmxAttribute);
            checkNotNull(getPassword(), passwordJmxAttribute);
        }

    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // FIXME better fix for Nodes datastore presence check
        ServiceReference<DataProviderService> serviceReference = bundleContext.getServiceReference(DataProviderService.class);
        DataProviderService dataProviderService =
                bundleContext.getService(serviceReference);
        dataProviderService.readOperationalData(InstanceIdentifier.builder(
                Nodes.class).child(Node.class).augmentation(NetconfNode.class).build());

        final RemoteDeviceId id = new RemoteDeviceId(getIdentifier());

        RemoteDeviceSalFacade salFacade = new NetconfDeviceSalFacade(id, getDomRegistryDependency(), bundleContext);
        final NetconfDevice device =
                NetconfDevice.createNetconfDevice(id, getGlobalNetconfSchemaProvider(), getGlobalProcessingExecutor(), salFacade);
        final NetconfDeviceCommunicator listener = new NetconfDeviceCommunicator(id, device, device);
        final NetconfClientConfiguration clientConfig = getClientConfig(listener);

        // FIXME BUG-944 remove backwards compatibility
        final NetconfClientDispatcher dispatcher = getClientDispatcher() == null ? createDispatcher() : getClientDispatcherDependency();
        listener.initializeRemoteConnection(dispatcher, clientConfig);

        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                device.close();
            }
        };
    }

    private ExecutorService getGlobalProcessingExecutor() {
        return GLOBAL_PROCESSING_EXECUTOR == null ? Executors.newCachedThreadPool() : GLOBAL_PROCESSING_EXECUTOR;
    }

    private synchronized AbstractCachingSchemaSourceProvider<String, InputStream> getGlobalNetconfSchemaProvider() {
        if(GLOBAL_NETCONF_SOURCE_PROVIDER == null) {
            final String storageFile = "cache/schema";
            //            File directory = bundleContext.getDataFile(storageFile);
            final File directory = new File(storageFile);
            final SchemaSourceProvider<String> defaultProvider = SchemaSourceProviders.noopProvider();
            GLOBAL_NETCONF_SOURCE_PROVIDER = FilesystemSchemaCachingProvider.createFromStringSourceProvider(defaultProvider, directory);
        }
        return GLOBAL_NETCONF_SOURCE_PROVIDER;
    }

    // FIXME BUG-944 remove backwards compatibility
    /**
     * @deprecated Use getClientDispatcherDependency method instead to retrieve injected dispatcher.
     * This one creates new instance of NetconfClientDispatcher and will be removed in near future.
     */
    @Deprecated
    private NetconfClientDispatcher createDispatcher() {
        return new NetconfClientDispatcherImpl(getBossThreadGroupDependency(), getWorkerThreadGroupDependency(), new HashedWheelTimer());
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public NetconfClientConfiguration getClientConfig(final NetconfDeviceCommunicator listener) {
        final InetSocketAddress socketAddress = getSocketAddress();
        final ReconnectStrategy strategy = getReconnectStrategy();
        final long clientConnectionTimeoutMillis = getConnectionTimeoutMillis();

        return NetconfClientConfigurationBuilder.create()
        .withAddress(socketAddress)
        .withConnectionTimeoutMillis(clientConnectionTimeoutMillis)
        .withReconnectStrategy(strategy)
        .withSessionListener(listener)
        .withAuthHandler(new LoginPassword(getUsername(),getPassword()))
        .withProtocol(getTcpOnly() ?
                NetconfClientConfiguration.NetconfClientProtocol.TCP :
                NetconfClientConfiguration.NetconfClientProtocol.SSH)
        .build();
    }

    private ReconnectStrategy getReconnectStrategy() {
        final Long connectionAttempts;
        if (getMaxConnectionAttempts() != null && getMaxConnectionAttempts() > 0) {
            connectionAttempts = getMaxConnectionAttempts();
        } else {
            logger.trace("Setting {} on {} to infinity", maxConnectionAttemptsJmxAttribute, this);
            connectionAttempts = null;
        }
        final double sleepFactor = 1.0;
        final int minSleep = 1000;
        final Long maxSleep = null;
        final Long deadline = null;

        return new TimedReconnectStrategy(GlobalEventExecutor.INSTANCE, getBetweenAttemptsTimeoutMillis(),
                minSleep, sleepFactor, maxSleep, connectionAttempts, deadline);
    }

    private InetSocketAddress getSocketAddress() {
        /*
         * Uncomment after Switch to IP Address
        if(getAddress().getIpv4Address() != null) {
            addressValue = getAddress().getIpv4Address().getValue();
        } else {
            addressValue = getAddress().getIpv6Address().getValue();
        }
         */
        final InetAddress inetAddress = InetAddresses.forString(getAddress());
        return new InetSocketAddress(inetAddress, getPort().intValue());
    }
}
