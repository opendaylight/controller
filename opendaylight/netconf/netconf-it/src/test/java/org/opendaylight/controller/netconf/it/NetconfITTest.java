/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.netty.channel.ChannelFuture;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.management.ObjectName;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.test.impl.DepTestImplModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.IdentityTestModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.MultipleDependenciesModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.MultipleDependenciesModuleMXBean;
import org.opendaylight.controller.config.yang.test.impl.NetconfTestImplModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.NetconfTestImplModuleMXBean;
import org.opendaylight.controller.config.yang.test.impl.TestImplModuleFactory;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcherImpl;
import org.opendaylight.controller.netconf.client.test.TestingNetconfClient;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.NetconfOperationServiceFactoryImpl;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreException;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcher;
import org.opendaylight.controller.netconf.impl.osgi.NetconfMonitoringServiceImpl;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceSnapshotImpl;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.test.types.rev131127.TestIdentity1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.test.types.rev131127.TestIdentity2;
import org.opendaylight.yangtools.yang.data.impl.codec.CodecRegistry;
import org.opendaylight.yangtools.yang.data.impl.codec.IdentityCodec;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class NetconfITTest extends AbstractNetconfConfigTest {

    // TODO refactor, pull common code up to AbstractNetconfITTest

    private static final InetSocketAddress tcpAddress = new InetSocketAddress("127.0.0.1", 12023);


    private NetconfMessage getConfig, getConfigCandidate, editConfig, closeSession;
    private DefaultCommitNotificationProducer commitNotificationProducer;
    private NetconfServerDispatcher dispatch;

    private NetconfClientDispatcherImpl clientDispatcher;

    static ModuleFactory[] FACTORIES = {new TestImplModuleFactory(), new DepTestImplModuleFactory(),
            new NetconfTestImplModuleFactory(), new IdentityTestModuleFactory(),
            new MultipleDependenciesModuleFactory()};

    @Before
    public void setUp() throws Exception {
        initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext,
                FACTORIES
        ));

        loadMessages();

        NetconfOperationServiceFactoryListenerImpl factoriesListener = new NetconfOperationServiceFactoryListenerImpl();
        factoriesListener.onAddNetconfOperationServiceFactory(new NetconfOperationServiceFactoryImpl(getYangStore()));

        commitNotificationProducer = new DefaultCommitNotificationProducer(ManagementFactory.getPlatformMBeanServer());

        dispatch = createDispatcher(factoriesListener);
        ChannelFuture s = dispatch.createServer(tcpAddress);
        s.await();

        clientDispatcher = new NetconfClientDispatcherImpl(getNettyThreadgroup(), getNettyThreadgroup(), getHashedWheelTimer());
    }

    private NetconfServerDispatcher createDispatcher(NetconfOperationServiceFactoryListenerImpl factoriesListener) {
        return super.createDispatcher(factoriesListener, getNetconfMonitoringListenerService(), commitNotificationProducer);
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
        commitNotificationProducer.close();
        clientDispatcher.close();
    }

    private void loadMessages() throws IOException, SAXException, ParserConfigurationException {
        this.editConfig = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/edit_config.xml");
        this.getConfig = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/getConfig.xml");
        this.getConfigCandidate = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/getConfig_candidate.xml");
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
        assertEquals("Some yang files were not found", Collections.<String>emptyList(), failedToFind);
        return yangDependencies;
    }


    @Test
    public void testNetconfClientDemonstration() throws Exception {
        try (TestingNetconfClient netconfClient = new TestingNetconfClient("client", clientDispatcher, getClientConfiguration(tcpAddress, 4000))) {

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
        try (TestingNetconfClient netconfClient = new TestingNetconfClient("1", clientDispatcher, getClientConfiguration(tcpAddress, 10000)))  {
            try (TestingNetconfClient netconfClient2 = new TestingNetconfClient("2", clientDispatcher, getClientConfiguration(tcpAddress, 10000))) {
                assertNotNull(netconfClient2.getCapabilities());
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

    private void assertIsOK(final Document rpcReply) throws NetconfDocumentedException {
        assertEquals("rpc-reply", rpcReply.getDocumentElement().getLocalName());
        assertEquals("ok", XmlElement.fromDomDocument(rpcReply).getOnlyChildElement().getName());
    }

    private Document assertGetConfigWorks(final TestingNetconfClient netconfClient) throws InterruptedException, ExecutionException, TimeoutException, NetconfDocumentedException {
        return assertGetConfigWorks(netconfClient, this.getConfig);
    }

    private Document assertGetConfigWorks(final TestingNetconfClient netconfClient, final NetconfMessage getConfigMessage)
            throws InterruptedException, ExecutionException, TimeoutException, NetconfDocumentedException {
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
        final TestingNetconfClient netconfClient = new TestingNetconfClient("test " + address.toString(), clientDispatcher, getClientConfiguration(address, 5000));
        assertEquals(expected, Long.toString(netconfClient.getSessionId()));
        return netconfClient;
    }

    @Test
    public void testIdRef() throws Exception {
        NetconfMessage editId = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/editConfig_identities.xml");
        NetconfMessage commit = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/commit.xml");

        try (TestingNetconfClient netconfClient = createSession(tcpAddress, "1")) {
            assertIsOK(netconfClient.sendMessage(editId).getDocument());
            assertIsOK(netconfClient.sendMessage(commit).getDocument());

            NetconfMessage response = netconfClient.sendMessage(getConfig);

            assertThat(XmlUtil.toString(response.getDocument()), JUnitMatchers.containsString("<afi xmlns:prefix=\"urn:opendaylight:params:xml:ns:yang:controller:config:test:types\">prefix:test-identity1</afi>"));
            assertThat(XmlUtil.toString(response.getDocument()), JUnitMatchers.containsString("<afi xmlns:prefix=\"urn:opendaylight:params:xml:ns:yang:controller:config:test:types\">prefix:test-identity2</afi>"));
            assertThat(XmlUtil.toString(response.getDocument()), JUnitMatchers.containsString("<safi xmlns:prefix=\"urn:opendaylight:params:xml:ns:yang:controller:config:test:types\">prefix:test-identity2</safi>"));
            assertThat(XmlUtil.toString(response.getDocument()), JUnitMatchers.containsString("<safi xmlns:prefix=\"urn:opendaylight:params:xml:ns:yang:controller:config:test:types\">prefix:test-identity1</safi>"));

        } catch (Exception e) {
            fail(Throwables.getStackTraceAsString(e));
        }
    }

    @Override
    protected CodecRegistry getCodecRegistry() {
        final IdentityCodec<?> codec = mock(IdentityCodec.class);
        doReturn(TestIdentity1.class).when(codec).deserialize(TestIdentity1.QNAME);
        doReturn(TestIdentity2.class).when(codec).deserialize(TestIdentity2.QNAME);

        final CodecRegistry ret = super.getCodecRegistry();
        doReturn(codec).when(ret).getIdentityCodec();
        return ret;
    }


    @Test
    public void testMultipleDependencies() throws Exception {
        // push first xml, should add parent and d1,d2 dependencies
        try (TestingNetconfClient netconfClient = createSession(tcpAddress, "1")) {
            Document rpcReply = netconfClient.sendMessage(
                    XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/editConfig_merge_multiple-deps1.xml"))
                    .getDocument();
            assertIsOK(rpcReply);
            commit(netconfClient);
        }
        // verify that parent.getTestingDeps == d1,d2
        MultipleDependenciesModuleMXBean parentProxy = configRegistryClient.newMXBeanProxy(
                configRegistryClient.lookupConfigBean(MultipleDependenciesModuleFactory.NAME, "parent"),
                MultipleDependenciesModuleMXBean.class);
        {
            List<ObjectName> testingDeps = parentProxy.getTestingDeps();
            assertEquals(2, testingDeps.size());
            Set<String> actualRefs = getServiceReferences(testingDeps);
            assertEquals(Sets.newHashSet("ref_d1", "ref_d2"), actualRefs);
        }

        // push second xml, should add d3 to parent's dependencies
        mergeD3(parentProxy);
        // push second xml again, to test that d3 is not added again
        mergeD3(parentProxy);
    }

    public void mergeD3(MultipleDependenciesModuleMXBean parentProxy) throws Exception {
        try (TestingNetconfClient netconfClient = new TestingNetconfClient(
                "test " + tcpAddress.toString(), clientDispatcher, getClientConfiguration(tcpAddress, 5000))) {

            Document rpcReply = netconfClient.sendMessage(
                    XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/editConfig_merge_multiple-deps2.xml"))
                    .getDocument();
            assertIsOK(rpcReply);
            commit(netconfClient);
        }
        {
            List<ObjectName> testingDeps = parentProxy.getTestingDeps();
            assertEquals(3, testingDeps.size());
            Set<String> actualRefs = getServiceReferences(testingDeps);
            assertEquals(Sets.newHashSet("ref_d1", "ref_d2", "ref_d3"), actualRefs);
        }
    }

    public Set<String> getServiceReferences(List<ObjectName> testingDeps) {
        return new HashSet<>(Lists.transform(testingDeps, new Function<ObjectName, String>() {
            @Override
            public String apply(ObjectName input) {
                return ObjectNameUtil.getReferenceName(input);
            }
        }));
    }

    public void commit(TestingNetconfClient netconfClient) throws Exception {
        Document rpcReply;
        rpcReply = netconfClient.sendMessage(XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/commit.xml"))
                .getDocument();
        assertIsOK(rpcReply);
    }
}
