/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.it;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.netty.channel.ChannelFuture;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.netconf.client.test.TestingNetconfClient;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreException;
import org.opendaylight.controller.config.yang.test.impl.DepTestImplModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.NetconfTestImplModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.NetconfTestImplModuleMXBean;
import org.opendaylight.controller.config.yang.test.impl.TestImplModuleFactory;
import org.opendaylight.controller.netconf.StubUserManager;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.NetconfOperationServiceFactoryImpl;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcher;
import org.opendaylight.controller.netconf.impl.osgi.NetconfMonitoringServiceImpl;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceSnapshotImpl;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.ssh.NetconfSSHServer;
import org.opendaylight.controller.netconf.ssh.authentication.AuthProvider;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.management.ObjectName;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class NetconfITTest extends AbstractNetconfConfigTest {

    // TODO refactor, pull common code up to AbstractNetconfITTest

    private static final Logger logger = LoggerFactory.getLogger(NetconfITTest.class);

    private static final InetSocketAddress tcpAddress = new InetSocketAddress("127.0.0.1", 12023);
    private static final InetSocketAddress sshAddress = new InetSocketAddress("127.0.0.1", 10830);
    private static final String USERNAME = "netconf";
    private static final String PASSWORD = "netconf";

    private NetconfMessage getConfig, getConfigCandidate, editConfig,
            closeSession, startExi, stopExi;
    private DefaultCommitNotificationProducer commitNot;
    private NetconfServerDispatcher dispatch;

    private NetconfClientDispatcher clientDispatcher;

    @Before
    public void setUp() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(getModuleFactories().toArray(
                new ModuleFactory[0])));

        loadMessages();

        NetconfOperationServiceFactoryListenerImpl factoriesListener = new NetconfOperationServiceFactoryListenerImpl();
        factoriesListener.onAddNetconfOperationServiceFactory(new NetconfOperationServiceFactoryImpl(getYangStore()));


        commitNot = new DefaultCommitNotificationProducer(ManagementFactory.getPlatformMBeanServer());

        dispatch = createDispatcher(factoriesListener);
        ChannelFuture s = dispatch.createServer(tcpAddress);
        s.await();

        clientDispatcher = new NetconfClientDispatcher(nettyThreadgroup, nettyThreadgroup, 5000);
    }

    private NetconfServerDispatcher createDispatcher(NetconfOperationServiceFactoryListenerImpl factoriesListener) {
        return super.createDispatcher(factoriesListener, getNetconfMonitoringListenerService(), commitNot);
    }

    static NetconfMonitoringServiceImpl getNetconfMonitoringListenerService() {
        NetconfOperationProvider netconfOperationProvider = mock(NetconfOperationProvider.class);
        NetconfOperationServiceSnapshotImpl snap = mock(NetconfOperationServiceSnapshotImpl.class);
        doReturn(Collections.<NetconfOperationService>emptySet()).when(snap).getServices();
        doReturn(snap).when(netconfOperationProvider).openSnapshot(anyString());
        return new NetconfMonitoringServiceImpl(netconfOperationProvider);
    }

    @After
    public void tearDown() throws Exception {
        commitNot.close();
        clientDispatcher.close();
    }

    private void loadMessages() throws IOException, SAXException, ParserConfigurationException {
        this.editConfig = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/edit_config.xml");
        this.getConfig = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/getConfig.xml");
        this.getConfigCandidate = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/getConfig_candidate.xml");
        this.startExi = XmlFileLoader
                .xmlFileToNetconfMessage("netconfMessages/startExi.xml");
        this.stopExi = XmlFileLoader
                .xmlFileToNetconfMessage("netconfMessages/stopExi.xml");
        this.closeSession = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/closeSession.xml");
    }

    private HardcodedYangStoreService getYangStore() throws YangStoreException, IOException {
        final Collection<InputStream> yangDependencies = getBasicYangs();
        return new HardcodedYangStoreService(yangDependencies);
    }

    static Collection<InputStream> getBasicYangs() throws IOException {

        List<String> paths = Arrays.asList("/META-INF/yang/config.yang", "/META-INF/yang/rpc-context.yang",
                "/META-INF/yang/config-test.yang", "/META-INF/yang/config-test-impl.yang", "/META-INF/yang/test-types.yang",
                "/META-INF/yang/ietf-inet-types.yang");
        final Collection<InputStream> yangDependencies = new ArrayList<>();
        List<String> failedToFind = new ArrayList<>();
        for (String path : paths) {
            InputStream resourceAsStream = NetconfITTest.class.getResourceAsStream(path);
            if (resourceAsStream == null) {
                failedToFind.add(path);
            } else {
                yangDependencies.add(resourceAsStream);
            }
        }
        assertEquals("Some yang files were not found", emptyList(), failedToFind);
        return yangDependencies;
    }

    protected List<ModuleFactory> getModuleFactories() {
        return getModuleFactoriesS();
    }

    static List<ModuleFactory> getModuleFactoriesS() {
        return Lists.newArrayList(new TestImplModuleFactory(), new DepTestImplModuleFactory(),
                new NetconfTestImplModuleFactory());
    }

    @Test
    public void testNetconfClientDemonstration() throws Exception {
        try (TestingNetconfClient netconfClient = new TestingNetconfClient("client", tcpAddress, 4000, clientDispatcher)) {

            Set<String> capabilitiesFromNetconfServer = netconfClient.getCapabilities();
            long sessionId = netconfClient.getSessionId();

            // NetconfMessage can be created :
            // new NetconfMessage(XmlUtil.readXmlToDocument("<xml/>"));

            NetconfMessage response = netconfClient.sendMessage(getConfig);
            response.getDocument();
        }
    }

    @Test
    public void testTwoSessions() throws Exception {
        try (TestingNetconfClient netconfClient = new TestingNetconfClient("1", tcpAddress, 10000, clientDispatcher))  {
            try (TestingNetconfClient netconfClient2 = new TestingNetconfClient("2", tcpAddress, 10000, clientDispatcher)) {
            }
        }
    }

    @Ignore
    @Test
    public void waitingTest() throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        transaction.createModule(DepTestImplModuleFactory.NAME, "eb");
        transaction.commit();
        Thread.currentThread().suspend();
    }

    @Test
    public void rpcReplyContainsAllAttributesTest() throws Exception {
        try (TestingNetconfClient netconfClient = createSession(tcpAddress, "1")) {
            final String rpc = "<rpc message-id=\"5\" a=\"a\" b=\"44\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">"
                    + "<get/>" + "</rpc>";
            final Document doc = XmlUtil.readXmlToDocument(rpc);
            final NetconfMessage message = netconfClient.sendMessage(new NetconfMessage(doc));
            assertNotNull(message);
            final NamedNodeMap expectedAttributes = doc.getDocumentElement().getAttributes();
            final NamedNodeMap returnedAttributes = message.getDocument().getDocumentElement().getAttributes();

            assertSameAttributes(expectedAttributes, returnedAttributes);
        }
    }

    private void assertSameAttributes(final NamedNodeMap expectedAttributes, final NamedNodeMap returnedAttributes) {
        assertNotNull("Expecting 4 attributes", returnedAttributes);
        assertEquals(expectedAttributes.getLength(), returnedAttributes.getLength());

        for (int i = 0; i < expectedAttributes.getLength(); i++) {
            final Node expAttr = expectedAttributes.item(i);
            final Node attr = returnedAttributes.item(i);
            assertEquals(expAttr.getNodeName(), attr.getNodeName());
            assertEquals(expAttr.getNamespaceURI(), attr.getNamespaceURI());
            assertEquals(expAttr.getTextContent(), attr.getTextContent());
        }
    }

    @Test
    public void rpcReplyErrorContainsAllAttributesTest() throws Exception {
        try (TestingNetconfClient netconfClient = createSession(tcpAddress, "1")) {
            final String rpc = "<rpc message-id=\"1\" a=\"adada\" b=\"4\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">"
                    + "<commit/>" + "</rpc>";
            final Document doc = XmlUtil.readXmlToDocument(rpc);
            final NetconfMessage message = netconfClient.sendMessage(new NetconfMessage(doc));
            final NamedNodeMap expectedAttributes = doc.getDocumentElement().getAttributes();
            final NamedNodeMap returnedAttributes = message.getDocument().getDocumentElement().getAttributes();

            assertSameAttributes(expectedAttributes, returnedAttributes);
        }
    }

    @Test
    public void rpcOutputContainsCorrectNamespace() throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        ObjectName dep = transaction.createModule(DepTestImplModuleFactory.NAME, "instanceD");
        ObjectName impl = transaction.createModule(NetconfTestImplModuleFactory.NAME, "instance");
        NetconfTestImplModuleMXBean proxy = configRegistryClient
                .newMXBeanProxy(impl, NetconfTestImplModuleMXBean.class);
        proxy.setTestingDep(dep);
        proxy.setSimpleShort((short) 0);

        transaction.commit();

        try (TestingNetconfClient netconfClient = createSession(tcpAddress, "1")) {
            final String expectedNamespace = "urn:opendaylight:params:xml:ns:yang:controller:test:impl";

            final String rpc = "<rpc message-id=\"5\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">"
                    + "<no-arg xmlns=\""
                    + expectedNamespace
                    + "\">    "
                    + "<context-instance>/modules/module[type='impl-netconf'][name='instance']</context-instance>"
                    + "<arg1>argument1</arg1>" + "</no-arg>" + "</rpc>";
            final Document doc = XmlUtil.readXmlToDocument(rpc);
            final NetconfMessage message = netconfClient.sendMessage(new NetconfMessage(doc));

            final Element rpcReply = message.getDocument().getDocumentElement();
            final XmlElement resultElement = XmlElement.fromDomElement(rpcReply).getOnlyChildElement();
            assertEquals("result", resultElement.getName());

            final String namespace = resultElement.getNamespaceAttribute();
            assertEquals(expectedNamespace, namespace);
        }
    }

    @Test
    public void testCloseSession() throws Exception {
        try (TestingNetconfClient netconfClient = createSession(tcpAddress, "1")) {

            // edit config
            Document rpcReply = netconfClient.sendMessage(this.editConfig)
                    .getDocument();
            assertIsOK(rpcReply);

            rpcReply = netconfClient.sendMessage(this.closeSession)
                    .getDocument();

            assertIsOK(rpcReply);
        }
    }

    @Test
    public void testEditConfig() throws Exception {
        try (TestingNetconfClient netconfClient = createSession(tcpAddress, "1")) {
            // send edit_config.xml
            final Document rpcReply = netconfClient.sendMessage(this.editConfig).getDocument();
            assertIsOK(rpcReply);
        }
    }

    @Test
    public void testValidate() throws Exception {
        try (TestingNetconfClient netconfClient = createSession(tcpAddress, "1")) {
            // begin transaction
            Document rpcReply = netconfClient.sendMessage(getConfigCandidate).getDocument();
            assertEquals("data", XmlElement.fromDomDocument(rpcReply).getOnlyChildElement().getName());

            // operations empty
            rpcReply = netconfClient.sendMessage(XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/validate.xml"))
                    .getDocument();
            assertIsOK(rpcReply);
        }
    }

    private void assertIsOK(final Document rpcReply) {
        assertEquals("rpc-reply", rpcReply.getDocumentElement().getLocalName());
        assertEquals("ok", XmlElement.fromDomDocument(rpcReply).getOnlyChildElement().getName());
    }

    private Document assertGetConfigWorks(final TestingNetconfClient netconfClient) throws InterruptedException, ExecutionException, TimeoutException {
        return assertGetConfigWorks(netconfClient, this.getConfig);
    }

    private Document assertGetConfigWorks(final TestingNetconfClient netconfClient, final NetconfMessage getConfigMessage)
            throws InterruptedException, ExecutionException, TimeoutException {
        final NetconfMessage rpcReply = netconfClient.sendMessage(getConfigMessage);
        assertNotNull(rpcReply);
        assertEquals("data", XmlElement.fromDomDocument(rpcReply.getDocument()).getOnlyChildElement().getName());
        return rpcReply.getDocument();
    }

    @Test
    public void testGetConfig() throws Exception {
        try (TestingNetconfClient netconfClient = createSession(tcpAddress, "1")) {
            assertGetConfigWorks(netconfClient);
        }
    }

    @Test
    public void createYangTestBasedOnYuma() throws Exception {
        try (TestingNetconfClient netconfClient = createSession(tcpAddress, "1")) {
            Document rpcReply = netconfClient.sendMessage(
                    XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/editConfig_merge_yang-test.xml"))
                    .getDocument();
            assertEquals("rpc-reply", rpcReply.getDocumentElement().getTagName());
            assertIsOK(rpcReply);
            assertGetConfigWorks(netconfClient, this.getConfigCandidate);
            rpcReply = netconfClient.sendMessage(XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/commit.xml"))
                    .getDocument();
            assertIsOK(rpcReply);

            final ObjectName on = new ObjectName(
                    "org.opendaylight.controller:instanceName=impl-dep-instance,type=Module,moduleFactoryName=impl-dep");
            Set<ObjectName> cfgBeans = configRegistryClient.lookupConfigBeans();
            assertEquals(cfgBeans, Sets.newHashSet(on));
        }
    }

    private TestingNetconfClient createSession(final InetSocketAddress address, final String expected) throws Exception {
        final TestingNetconfClient netconfClient = new TestingNetconfClient("test " + address.toString(), address, 5000, clientDispatcher);
        assertEquals(expected, Long.toString(netconfClient.getSessionId()));
        return netconfClient;
    }

    private void startSSHServer() throws Exception {
        logger.info("Creating SSH server");
        StubUserManager um = new StubUserManager(USERNAME, PASSWORD);
        String pem;
        try (InputStream is = getClass().getResourceAsStream("/RSA.pk")) {
            pem = IOUtils.toString(is);
        }
        AuthProvider ap = new AuthProvider(um, pem);
        Thread sshServerThread = new Thread(NetconfSSHServer.start(10830, tcpAddress, ap));
        sshServerThread.setDaemon(true);
        sshServerThread.start();
        logger.info("SSH server on");
    }

    @Test
    public void sshTest() throws Exception {
        startSSHServer();
        logger.info("creating connection");
        Connection conn = new Connection(sshAddress.getHostName(), sshAddress.getPort());
        Assert.assertNotNull(conn);
        logger.info("connection created");
        conn.connect();
        boolean isAuthenticated = conn.authenticateWithPassword(USERNAME, PASSWORD);
        assertTrue(isAuthenticated);
        logger.info("user authenticated");
        final Session sess = conn.openSession();
        sess.startSubSystem("netconf");
        logger.info("user authenticated");
        sess.getStdin().write(XmlUtil.toString(this.getConfig.getDocument()).getBytes());

        new Thread() {
            @Override
            public void run() {
                while (true) {
                    byte[] bytes = new byte[1024];
                    int c = 0;
                    try {
                        c = sess.getStdout().read(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    logger.info("got data:" + bytes);
                    if (c == 0) {
                        break;
                    }
                }
            }
        }.join();
    }

}
