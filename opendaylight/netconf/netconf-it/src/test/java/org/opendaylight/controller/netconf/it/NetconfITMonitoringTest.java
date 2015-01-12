/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.it;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.opendaylight.controller.netconf.util.test.XmlUnitUtil.assertContainsElementWithText;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.monitoring.NetconfManagementSession;
import org.opendaylight.controller.netconf.client.TestingNetconfClient;
import org.opendaylight.controller.netconf.impl.osgi.NetconfMonitoringServiceImpl;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceSnapshotImpl;
import org.opendaylight.controller.netconf.impl.osgi.SessionMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.Capability;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.monitoring.osgi.NetconfMonitoringActivator;
import org.opendaylight.controller.netconf.monitoring.osgi.NetconfMonitoringOperationService;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.w3c.dom.Document;

public class NetconfITMonitoringTest extends AbstractNetconfConfigTest {

    public static final int PORT = 12025;
    public static final InetSocketAddress TCP_ADDRESS = new InetSocketAddress(LOOPBACK_ADDRESS, PORT);
    public static final TestingCapability TESTING_CAPABILITY = new TestingCapability();

    private NetconfMonitoringServiceImpl netconfMonitoringService;

    @Override
    protected void setUpTestInitial() {
        netconfMonitoringService = new NetconfMonitoringServiceImpl(getNetconfOperationProvider());
    }

    @Override
    protected SessionMonitoringService getNetconfMonitoringService() throws Exception {
        return netconfMonitoringService;
    }

    @Override
    protected Iterable<NetconfOperationServiceFactory> getAdditionalServiceFactories() {
        return Collections.<NetconfOperationServiceFactory>singletonList(new NetconfMonitoringActivator.NetconfMonitoringOperationServiceFactory(
                new NetconfMonitoringOperationService(netconfMonitoringService)));
    }

    @Override
    protected InetSocketAddress getTcpServerAddress() {
        return TCP_ADDRESS;
    }

    static SessionMonitoringService getNetconfMonitoringListenerService(final Logger LOG, final NetconfMonitoringServiceImpl monitor) {
        return new SessionMonitoringService() {
            @Override
            public void onSessionUp(final NetconfManagementSession session) {
                LOG.debug("Management session up {}", session);
                monitor.onSessionUp(session);
            }

            @Override
            public void onSessionDown(final NetconfManagementSession session) {
                LOG.debug("Management session down {}", session);
                monitor.onSessionDown(session);
            }
        };
    }

    @Test
    public void testGetResponseFromMonitoring() throws Exception {
        try (TestingNetconfClient netconfClient = new TestingNetconfClient("client-monitoring", getClientDispatcher(), getClientConfiguration(TCP_ADDRESS, 10000))) {
            try (TestingNetconfClient netconfClient2 = new TestingNetconfClient("client-monitoring2", getClientDispatcher(), getClientConfiguration(TCP_ADDRESS, 10000))) {
                Thread.sleep(500);
                final NetconfMessage response = netconfClient2.sendMessage(getGet());
                assertSessionElementsInResponse(response.getDocument(), 2);
            }
            Thread.sleep(500);
            final NetconfMessage response = netconfClient.sendMessage(getGet());
            assertSessionElementsInResponse(response.getDocument(), 1);
        }
    }


    @Test(timeout = 13 * 10000)
    public void testClientHelloWithAuth() throws Exception {
        String fileName = "netconfMessages/client_hello_with_auth.xml";
        final String hello = XmlFileLoader.fileToString(fileName);

        fileName = "netconfMessages/get.xml";
        final String get = XmlFileLoader.fileToString(fileName);

        final Socket sock = new Socket(TCP_ADDRESS.getHostName(), TCP_ADDRESS.getPort());
        sock.getOutputStream().write(hello.getBytes(Charsets.UTF_8));
        final String separator = "]]>]]>";

        sock.getOutputStream().write(separator.getBytes(Charsets.UTF_8));
        sock.getOutputStream().write(get.getBytes(Charsets.UTF_8));
        sock.getOutputStream().write(separator.getBytes(Charsets.UTF_8));

        final StringBuilder responseBuilder = new StringBuilder();

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

        final String helloMsg = responseBuilder.substring(0, responseBuilder.indexOf(separator));
        Document doc = XmlUtil.readXmlToDocument(helloMsg);
        assertContainsElementWithText(doc, "urn:ietf:params:netconf:capability:candidate:1.0");

        final String replyMsg = responseBuilder.substring(responseBuilder.indexOf(separator) + separator.length());
        doc = XmlUtil.readXmlToDocument(replyMsg);
        assertContainsElementWithText(doc, "tomas");
    }

    private void assertSessionElementsInResponse(final Document document, final int i) {
        final int elementSize = document.getElementsByTagName("session-id").getLength();
        assertEquals("Incorrect number of session-id tags in " + XmlUtil.toString(document), i, elementSize);
    }

    public static NetconfOperationProvider getNetconfOperationProvider() {
        final NetconfOperationProvider factoriesListener = mock(NetconfOperationProvider.class);
        final NetconfOperationServiceSnapshotImpl snap = mock(NetconfOperationServiceSnapshotImpl.class);
        try {
            doNothing().when(snap).close();
        } catch (final Exception e) {
            // not happening
            throw new IllegalStateException(e);
        }
        final NetconfOperationService service = mock(NetconfOperationService.class);
        final Set<Capability> caps = Sets.newHashSet();
        caps.add(TESTING_CAPABILITY);

        doReturn(caps).when(service).getCapabilities();
        final Set<NetconfOperationService> services = Sets.newHashSet(service);
        doReturn(services).when(snap).getServices();
        doReturn(snap).when(factoriesListener).openSnapshot(anyString());

        return factoriesListener;
    }

    private static class TestingCapability implements Capability {
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
        public List<String> getLocation() {
            return Collections.emptyList();
        }
    }
}
