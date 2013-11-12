/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.it;

import com.google.common.base.Optional;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.yang.store.api.YangStoreException;
import org.opendaylight.controller.config.yang.store.impl.HardcodedYangStoreService;
import org.opendaylight.controller.netconf.client.NetconfClient;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.NetconfOperationServiceFactoryImpl;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcher;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionListenerFactory;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionNegotiatorFactory;
import org.opendaylight.controller.netconf.impl.SessionIdProvider;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;
import org.opendaylight.protocol.util.SSLUtil;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NetconfITSecureTest extends AbstractConfigTest {

    private static final InetSocketAddress tlsAddress = new InetSocketAddress("127.0.0.1", 12024);

    private DefaultCommitNotificationProducer commitNot;
    private NetconfServerDispatcher dispatchS;
    private EventLoopGroup nettyThreadgroup;


    @Before
    public void setUp() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(getModuleFactories().toArray(
                new ModuleFactory[0])));

        NetconfOperationServiceFactoryListenerImpl factoriesListener = new NetconfOperationServiceFactoryListenerImpl();
        factoriesListener.onAddNetconfOperationServiceFactory(new NetconfOperationServiceFactoryImpl(getYangStore()));

        commitNot = new DefaultCommitNotificationProducer(ManagementFactory.getPlatformMBeanServer());

        nettyThreadgroup = new NioEventLoopGroup();

        dispatchS = createDispatcher(Optional.of(getSslContext()), factoriesListener);
        ChannelFuture s = dispatchS.createServer(tlsAddress);
        s.await();
    }

    private NetconfServerDispatcher createDispatcher(Optional<SSLContext> sslC,
            NetconfOperationServiceFactoryListenerImpl factoriesListener) {
        SessionIdProvider idProvider = new SessionIdProvider();
        NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(
                new HashedWheelTimer(5000, TimeUnit.MILLISECONDS), factoriesListener, idProvider);

        NetconfServerSessionListenerFactory listenerFactory = new NetconfServerSessionListenerFactory(
                factoriesListener, commitNot, idProvider);

        NetconfServerDispatcher.ServerSslChannelInitializer serverChannelInitializer = new NetconfServerDispatcher.ServerSslChannelInitializer(
                sslC, serverNegotiatorFactory, listenerFactory);
        return new NetconfServerDispatcher(serverChannelInitializer, nettyThreadgroup, nettyThreadgroup);
    }

    @After
    public void tearDown() throws Exception {
        commitNot.close();
        nettyThreadgroup.shutdownGracefully();
    }

    private SSLContext getSslContext() throws KeyStoreException, NoSuchAlgorithmException, CertificateException,
            IOException, UnrecoverableKeyException, KeyManagementException {
        final InputStream keyStore = getClass().getResourceAsStream("/keystore.jks");
        final InputStream trustStore = getClass().getResourceAsStream("/keystore.jks");
        SSLContext sslContext = SSLUtil.initializeSecureContext("password", keyStore, trustStore, KeyManagerFactory.getDefaultAlgorithm());
        keyStore.close();
        trustStore.close();
        return sslContext;
    }

    private HardcodedYangStoreService getYangStore() throws YangStoreException, IOException {
        final Collection<InputStream> yangDependencies = NetconfITTest.getBasicYangs();
        return new HardcodedYangStoreService(yangDependencies);
    }

    protected List<ModuleFactory> getModuleFactories() {
        return NetconfITTest.getModuleFactoriesS();
    }

    @Test
    public void testSecure() throws Exception {
        NetconfClientDispatcher dispatch = new NetconfClientDispatcher(Optional.of(getSslContext()), nettyThreadgroup, nettyThreadgroup);
        try (NetconfClient netconfClient = new NetconfClient("tls-client", tlsAddress, 4000, dispatch))  {

        }
    }
}
