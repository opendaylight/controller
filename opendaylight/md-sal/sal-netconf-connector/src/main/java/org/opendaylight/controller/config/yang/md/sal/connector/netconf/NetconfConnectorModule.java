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

import com.google.common.net.InetAddresses;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcherImpl;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfigurationBuilder;
import org.opendaylight.controller.netconf.util.handler.ssh.authentication.LoginPassword;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.connect.netconf.InventoryUtils;
import org.opendaylight.controller.sal.connect.netconf.NetconfDevice;
import org.opendaylight.controller.sal.connect.netconf.NetconfDeviceListener;
import org.opendaylight.controller.sal.core.api.data.DataChangeListener;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.protocol.framework.TimedReconnectStrategy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.inventory.rev140108.NetconfNode;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.util.repo.AbstractCachingSchemaSourceProvider;
import org.opendaylight.yangtools.yang.model.util.repo.FilesystemSchemaCachingProvider;
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProvider;
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProviders;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public final class NetconfConnectorModule extends org.opendaylight.controller.config.yang.md.sal.connector.netconf.AbstractNetconfConnectorModule
{
    private static final Logger logger = LoggerFactory.getLogger(NetconfConnectorModule.class);

    private static ExecutorService GLOBAL_PROCESSING_EXECUTOR = null;
    private static AbstractCachingSchemaSourceProvider<String, InputStream> GLOBAL_NETCONF_SOURCE_PROVIDER = null;
    private BundleContext bundleContext;

    public NetconfConnectorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfConnectorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, NetconfConnectorModule oldModule, java.lang.AutoCloseable oldInstance) {
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
        ServiceReference<DataProviderService> serviceReference = bundleContext.getServiceReference(DataProviderService.class);

        DataProviderService dataProviderService =
                bundleContext.getService(serviceReference);

        getDomRegistryDependency();
        NetconfDevice device = new NetconfDevice(getIdentifier().getInstanceName());

        device.setClientConfig(getClientConfig(device));

        device.setProcessingExecutor(getGlobalProcessingExecutor());

        device.setEventExecutor(getEventExecutorDependency());
        device.setDispatcher(getClientDispatcher() == null ? createDispatcher() : getClientDispatcherDependency());
        device.setSchemaSourceProvider(getGlobalNetconfSchemaProvider(bundleContext));
        device.setDataProviderService(dataProviderService);
        getDomRegistryDependency().registerProvider(device, bundleContext);
        device.start();
        return device;
    }

    private ExecutorService getGlobalProcessingExecutor() {
        return GLOBAL_PROCESSING_EXECUTOR == null ? Executors.newCachedThreadPool() : GLOBAL_PROCESSING_EXECUTOR;
    }

    private synchronized AbstractCachingSchemaSourceProvider<String, InputStream> getGlobalNetconfSchemaProvider(BundleContext bundleContext) {
        if(GLOBAL_NETCONF_SOURCE_PROVIDER == null) {
            String storageFile = "cache/schema";
            //            File directory = bundleContext.getDataFile(storageFile);
            File directory = new File(storageFile);
            SchemaSourceProvider<String> defaultProvider = SchemaSourceProviders.noopProvider();
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

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public NetconfReconnectingClientConfiguration getClientConfig(final NetconfDevice device) {
        InetSocketAddress socketAddress = getSocketAddress();
        ReconnectStrategy strategy = getReconnectStrategy();
        long clientConnectionTimeoutMillis = getConnectionTimeoutMillis();

        return NetconfReconnectingClientConfigurationBuilder.create()
        .withAddress(socketAddress)
        .withConnectionTimeoutMillis(clientConnectionTimeoutMillis)
        .withReconnectStrategy(strategy)
        .withSessionListener(new NetconfDeviceListener(device))
        .withAuthHandler(new LoginPassword(getUsername(),getPassword()))
        .withProtocol(getTcpOnly() ?
                NetconfClientConfiguration.NetconfClientProtocol.TCP :
                NetconfClientConfiguration.NetconfClientProtocol.SSH)
        .withConnectStrategyFactory(new ReconnectStrategyFactory() {
            @Override
            public ReconnectStrategy createReconnectStrategy() {
                return getReconnectStrategy();
            }
        })
        .build();
    }

    private ReconnectStrategy getReconnectStrategy() {
        Long connectionAttempts;
        if (getMaxConnectionAttempts() != null && getMaxConnectionAttempts() > 0) {
            connectionAttempts = getMaxConnectionAttempts();
        } else {
            logger.trace("Setting {} on {} to infinity", maxConnectionAttemptsJmxAttribute, this);
            connectionAttempts = null;
        }
        double sleepFactor = 1.5;
        int minSleep = 1000;
        Long maxSleep = null;
        Long deadline = null;

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
        InetAddress inetAddress = InetAddresses.forString(getAddress());
        return new InetSocketAddress(inetAddress, getPort().intValue());
    }
}
