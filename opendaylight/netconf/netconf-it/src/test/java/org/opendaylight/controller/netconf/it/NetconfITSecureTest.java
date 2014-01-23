/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.it;

import io.netty.channel.ChannelFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.yang.store.api.YangStoreException;
import org.opendaylight.controller.config.yang.store.impl.HardcodedYangStoreService;
import org.opendaylight.controller.netconf.client.NetconfClient;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.NetconfOperationServiceFactoryImpl;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcher;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;

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

public class NetconfITSecureTest extends AbstractNetconfConfigTest {

    private static final InetSocketAddress tlsAddress = new InetSocketAddress("127.0.0.1", 12024);

    private DefaultCommitNotificationProducer commitNot;
    private NetconfServerDispatcher dispatchS;


    @Before
    public void setUp() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(getModuleFactories().toArray(
                new ModuleFactory[0])));

        NetconfOperationServiceFactoryListenerImpl factoriesListener = new NetconfOperationServiceFactoryListenerImpl();
        factoriesListener.onAddNetconfOperationServiceFactory(new NetconfOperationServiceFactoryImpl(getYangStore()));

        commitNot = new DefaultCommitNotificationProducer(ManagementFactory.getPlatformMBeanServer());


        dispatchS = createDispatcher(factoriesListener);
        ChannelFuture s = dispatchS.createServer(tlsAddress);
        s.await();
    }

    private NetconfServerDispatcher createDispatcher(NetconfOperationServiceFactoryListenerImpl factoriesListener) {
        return super.createDispatcher(factoriesListener, NetconfITTest.getNetconfMonitoringListenerService(), commitNot);
    }

    @After
    public void tearDown() throws Exception {
        commitNot.close();
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
        NetconfClientDispatcher dispatch = new NetconfClientDispatcher(nettyThreadgroup, nettyThreadgroup, 5000);
        try (NetconfClient netconfClient = new NetconfClient("tls-client", tlsAddress, 4000, dispatch))  {

        }
    }
}
