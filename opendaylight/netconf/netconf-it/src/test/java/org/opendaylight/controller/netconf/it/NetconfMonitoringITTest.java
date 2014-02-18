/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.it;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import io.netty.channel.ChannelFuture;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.mockito.Mock;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.yang.store.api.YangStoreException;
import org.opendaylight.controller.config.yang.store.impl.HardcodedYangStoreService;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.monitoring.NetconfManagementSession;
import org.opendaylight.controller.netconf.client.NetconfClient;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.NetconfOperationServiceFactoryImpl;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcher;
import org.opendaylight.controller.netconf.impl.osgi.NetconfMonitoringServiceImpl;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListener;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceSnapshot;
import org.opendaylight.controller.netconf.impl.osgi.SessionMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.Capability;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.monitoring.osgi.NetconfMonitoringActivator;
import org.opendaylight.controller.netconf.monitoring.osgi.NetconfMonitoringOperationService;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;

public class NetconfMonitoringITTest extends AbstractNetconfConfigTest {

    private static final Logger logger =  LoggerFactory.getLogger(NetconfITTest.class);

    private static final InetSocketAddress tcpAddress = new InetSocketAddress("127.0.0.1", 12023);

    @Mock
    private DefaultCommitNotificationProducer commitNot;
    private NetconfServerDispatcher dispatch;

    private NetconfClientDispatcher clientDispatcher;

    private NetconfMonitoringServiceImpl monitoringService;

    @Before
    public void setUp() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(NetconfITTest.getModuleFactoriesS().toArray(
                new ModuleFactory[0])));

        monitoringService = new NetconfMonitoringServiceImpl(getFactoriesListener());

        NetconfOperationServiceFactoryListenerImpl factoriesListener = new NetconfOperationServiceFactoryListenerImpl();
        factoriesListener.onAddNetconfOperationServiceFactory(new NetconfOperationServiceFactoryImpl(getYangStore()));
        factoriesListener
                .onAddNetconfOperationServiceFactory(new NetconfMonitoringActivator.NetconfMonitoringOperationServiceFactory(
                        new NetconfMonitoringOperationService(monitoringService)));


        dispatch = createDispatcher(factoriesListener);
        ChannelFuture s = dispatch.createServer(tcpAddress);
        s.await();

        clientDispatcher = new NetconfClientDispatcher(nettyThreadgroup, nettyThreadgroup, 5000);
    }

    private HardcodedYangStoreService getYangStore() throws YangStoreException, IOException {
        final Collection<InputStream> yangDependencies = NetconfITTest.getBasicYangs();
        return new HardcodedYangStoreService(yangDependencies);
    }

    private NetconfServerDispatcher createDispatcher(
                                                     NetconfOperationServiceFactoryListenerImpl factoriesListener) {
        return super.createDispatcher(factoriesListener, getNetconfMonitoringListenerService(logger, monitoringService), commitNot);
    }

    static SessionMonitoringService getNetconfMonitoringListenerService(final Logger logger, final NetconfMonitoringServiceImpl monitor) {
        return new SessionMonitoringService() {
            @Override
            public void onSessionUp(NetconfManagementSession session) {
                logger.debug("Management session up {}", session);
                monitor.onSessionUp(session);
            }

            @Override
            public void onSessionDown(NetconfManagementSession session) {
                logger.debug("Management session down {}", session);
                monitor.onSessionDown(session);
            }
        };
    }


    @Test
    public void testGetResponseFromMonitoring() throws Exception {
        try (NetconfClient netconfClient = new NetconfClient("client-monitoring", tcpAddress, 4000, clientDispatcher)) {
        try (NetconfClient netconfClient2 = new NetconfClient("client-monitoring2", tcpAddress, 4000, clientDispatcher)) {
            NetconfMessage response = netconfClient.sendMessage(loadGetMessage());
            assertSessionElementsInResponse(response.getDocument(), 2);
        }
            NetconfMessage response = netconfClient.sendMessage(loadGetMessage());
            assertSessionElementsInResponse(response.getDocument(), 1);
        }
    }


    @Test(timeout = 5 * 10000)
    public void testClientHelloWithAuth() throws Exception {
        String fileName = "netconfMessages/client_hello_with_auth.xml";
        String hello = XmlFileLoader.fileToString(fileName);

        fileName = "netconfMessages/get.xml";
        String get = XmlFileLoader.fileToString(fileName);

        Socket sock = new Socket(tcpAddress.getHostName(), tcpAddress.getPort());
        sock.getOutputStream().write(hello.getBytes(Charsets.UTF_8));
        String separator = "]]>]]>";

        sock.getOutputStream().write(separator.getBytes(Charsets.UTF_8));
        sock.getOutputStream().write(get.getBytes(Charsets.UTF_8));
        sock.getOutputStream().write(separator.getBytes(Charsets.UTF_8));

        StringBuilder responseBuilder = new StringBuilder();

        try (InputStream inputStream = sock.getInputStream();
             InputStreamReader reader = new InputStreamReader(inputStream);
             BufferedReader buff = new BufferedReader(reader)) {
            String line;
            while ((line = buff.readLine()) != null) {

                responseBuilder.append(line);
                responseBuilder.append(System.lineSeparator());

                if(line.contains("</rpc-reply>"))
                    break;
            }
        }

        sock.close();

        org.junit.Assert.assertThat(responseBuilder.toString(), JUnitMatchers.containsString("<capability>urn:ietf:params:netconf:capability:candidate:1.0</capability>"));
        org.junit.Assert.assertThat(responseBuilder.toString(), JUnitMatchers.containsString("<username>tomas</username>"));
    }

    private void assertSessionElementsInResponse(Document document, int i) {
        int elementSize = document.getElementsByTagName("session-id").getLength();
        Assert.assertEquals(i, elementSize);
    }

    private NetconfMessage loadGetMessage() throws Exception {
        return XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/get.xml");
    }

    public static NetconfOperationServiceFactoryListener getFactoriesListener() {
        NetconfOperationServiceFactoryListener factoriesListener = mock(NetconfOperationServiceFactoryListener.class);
        NetconfOperationServiceSnapshot snap = mock(NetconfOperationServiceSnapshot.class);
        NetconfOperationService service = mock(NetconfOperationService.class);
        Set<Capability> caps = Sets.newHashSet();
        caps.add(new Capability() {
            @Override
            public String getCapabilityUri() {
                return "namespaceModuleRevision";
            }

            @Override
            public Optional<String> getModuleNamespace() {
                return Optional.of("namespace");
            }

            @Override
            public Optional<String> getModuleName() {
                return Optional.of("name");
            }

            @Override
            public Optional<String> getRevision() {
                return Optional.of("revision");
            }

            @Override
            public Optional<String> getCapabilitySchema() {
                return Optional.of("content");
            }

            @Override
            public Optional<List<String>> getLocation() {
                return Optional.absent();
            }
        });

        doReturn(caps).when(service).getCapabilities();
        Set<NetconfOperationService> services = Sets.newHashSet(service);
        doReturn(services).when(snap).getServices();
        doReturn(snap).when(factoriesListener).getSnapshot(anyLong());

        return factoriesListener;
    }


}
