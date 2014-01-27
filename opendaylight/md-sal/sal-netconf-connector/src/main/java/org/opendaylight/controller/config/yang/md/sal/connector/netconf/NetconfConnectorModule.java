/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.connector.netconf;

import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;

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

import static com.google.common.base.Preconditions.*;

import com.google.common.base.Optional;
import com.google.common.net.InetAddresses;

/**
*
*/
public final class NetconfConnectorModule extends org.opendaylight.controller.config.yang.md.sal.connector.netconf.AbstractNetconfConnectorModule
{

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
    public void validate(){
        super.validate();
        checkState(getAddress() != null,"Address must be set.");
        //checkState(getAddress().getIpv4Address() != null || getAddress().getIpv6Address() != null,"Address must be set.");
        checkState(getPort() != null,"Port must be set.");
        checkState(getDomRegistry() != null,"Dom Registry must be provided.");
    }


    @Override
    public java.lang.AutoCloseable createInstance() {
        
        getDomRegistryDependency();
        NetconfDevice device = new NetconfDevice(getIdentifier().getInstanceName());
        String addressValue = getAddress();
        
        
        int attemptMsTimeout = 60*1000;
        int connectionAttempts = 5;
        /*
         * Uncomment after Switch to IP Address
        if(getAddress().getIpv4Address() != null) {
            addressValue = getAddress().getIpv4Address().getValue();
        } else {
            addressValue = getAddress().getIpv6Address().getValue();
        }
        */
        ReconnectStrategy strategy = new TimedReconnectStrategy(GlobalEventExecutor.INSTANCE, attemptMsTimeout, 1000, 1.0, null,
                Long.valueOf(connectionAttempts), null);
        
        device.setReconnectStrategy(strategy);
        
        InetAddress addr = InetAddresses.forString(addressValue);
        InetSocketAddress socketAddress = new InetSocketAddress(addr , getPort().intValue());

        
        device.setProcessingExecutor(getGlobalProcessingExecutor());
        
        device.setSocketAddress(socketAddress);
        device.setEventExecutor(getEventExecutorDependency());
        device.setDispatcher(createDispatcher());
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
            File directory = new File("cache/schema");
            SchemaSourceProvider<String> defaultProvider = SchemaSourceProviders.noopProvider();
            GLOBAL_NETCONF_SOURCE_PROVIDER = FilesystemSchemaCachingProvider.createFromStringSourceProvider(defaultProvider, directory);
        }
        return GLOBAL_NETCONF_SOURCE_PROVIDER;
    }

    private NetconfClientDispatcher createDispatcher() {
        EventLoopGroup bossGroup = getBossThreadGroupDependency();
        EventLoopGroup workerGroup = getWorkerThreadGroupDependency();
        if(getTcpOnly()) {
            return new NetconfClientDispatcher( bossGroup, workerGroup);
        } else {
            AuthenticationHandler authHandler = new LoginPassword(getUsername(),getPassword());
            return new NetconfSshClientDispatcher(authHandler , bossGroup, workerGroup);
        }
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
