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
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.NetconfSshClientDispatcher;
import org.opendaylight.controller.netconf.util.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.util.handler.ssh.authentication.LoginPassword;
import org.opendaylight.controller.sal.connect.netconf.NetconfDevice;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.TimedReconnectStrategy;
import org.opendaylight.yangtools.yang.model.util.repo.AbstractCachingSchemaSourceProvider;
import org.opendaylight.yangtools.yang.model.util.repo.FilesystemSchemaCachingProvider;
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProvider;
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProviders;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InetAddresses;

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

    }

    @Override
    public java.lang.AutoCloseable createInstance() {

        getDomRegistryDependency();
        NetconfDevice device = new NetconfDevice(getIdentifier().getInstanceName());
        String addressValue = getAddress();

        Long connectionAttempts;
        if (getMaxConnectionAttempts() != null && getMaxConnectionAttempts() > 0) {
            connectionAttempts = getMaxConnectionAttempts();
        } else {
            logger.trace("Setting {} on {} to infinity", maxConnectionAttemptsJmxAttribute, this);
            connectionAttempts = null;
        }
        long clientConnectionTimeoutMillis = getConnectionTimeoutMillis();
        /*
         * Uncomment after Switch to IP Address
        if(getAddress().getIpv4Address() != null) {
            addressValue = getAddress().getIpv4Address().getValue();
        } else {
            addressValue = getAddress().getIpv6Address().getValue();
        }
         */
        double sleepFactor = 1.0;
        int minSleep = 1000;
        Long maxSleep = null;
        Long deadline = null;
        ReconnectStrategy strategy = new TimedReconnectStrategy(GlobalEventExecutor.INSTANCE, getBetweenAttemptsTimeoutMillis(),
                minSleep, sleepFactor, maxSleep, connectionAttempts, deadline);

        device.setReconnectStrategy(strategy);

        InetAddress addr = InetAddresses.forString(addressValue);
        InetSocketAddress socketAddress = new InetSocketAddress(addr , getPort().intValue());


        device.setProcessingExecutor(getGlobalProcessingExecutor());

        device.setSocketAddress(socketAddress);
        device.setEventExecutor(getEventExecutorDependency());
        device.setDispatcher(createDispatcher(clientConnectionTimeoutMillis));
        device.setSchemaSourceProvider(getGlobalNetconfSchemaProvider(bundleContext));

        getDomRegistryDependency().registerProvider(device, bundleContext);
        device.start();
        return device;
    }

    private ExecutorService getGlobalProcessingExecutor() {
        if(GLOBAL_PROCESSING_EXECUTOR == null) {

            GLOBAL_PROCESSING_EXECUTOR = Executors.newCachedThreadPool();

        }
        return GLOBAL_PROCESSING_EXECUTOR;
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

    private NetconfClientDispatcher createDispatcher(long clientConnectionTimeoutMillis) {
        EventLoopGroup bossGroup = getBossThreadGroupDependency();
        EventLoopGroup workerGroup = getWorkerThreadGroupDependency();
        if(getTcpOnly()) {
            return new NetconfClientDispatcher( bossGroup, workerGroup, clientConnectionTimeoutMillis);
        } else {
            AuthenticationHandler authHandler = new LoginPassword(getUsername(),getPassword());
            return new NetconfSshClientDispatcher(authHandler , bossGroup, workerGroup, clientConnectionTimeoutMillis);
        }
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
