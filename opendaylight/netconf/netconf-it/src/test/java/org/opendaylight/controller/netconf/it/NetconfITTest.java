/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.it;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.management.ObjectName;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.test.impl.DepTestImplModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.MultipleDependenciesModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.MultipleDependenciesModuleMXBean;
import org.opendaylight.controller.config.yang.test.impl.NetconfTestImplModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.NetconfTestImplModuleMXBean;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.TestingNetconfClient;
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

    public static final int PORT = 12023;
    public static final InetSocketAddress TCP_ADDRESS = new InetSocketAddress(LOOPBACK_ADDRESS, PORT);

    private NetconfMessage getConfigCandidate, editConfig, closeSession;
    private NetconfClientDispatcher clientDispatcher;

    @Before
    public void setUp() throws Exception {
        loadMessages();
        clientDispatcher = getClientDispatcher();
    }

    @Override
    protected InetSocketAddress getTcpServerAddress() {
        return TCP_ADDRESS;
    }

    private void loadMessages() throws IOException, SAXException, ParserConfigurationException {
        this.editConfig = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/edit_config.xml");
        this.getConfigCandidate = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/getConfig_candidate.xml");
        this.closeSession = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/closeSession.xml");
    }

    @Test
    public void testNetconfClientDemonstration() throws Exception {
        try (TestingNetconfClient netconfClient = new TestingNetconfClient("client", clientDispatcher, getClientConfiguration(TCP_ADDRESS, 4000))) {

            final Set<String> capabilitiesFromNetconfServer = netconfClient.getCapabilities();
            final long sessionId = netconfClient.getSessionId();

            // NetconfMessage can be created :
            // new NetconfMessage(XmlUtil.readXmlToDocument("<xml/>"));

            final NetconfMessage response = netconfClient.sendMessage(getGetConfig());
            response.getDocument();
        }
    }

    @Test
    public void testTwoSessions() throws Exception {
        try (TestingNetconfClient netconfClient = new TestingNetconfClient("1", clientDispatcher, getClientConfiguration(TCP_ADDRESS, 10000)))  {
            try (TestingNetconfClient netconfClient2 = new TestingNetconfClient("2", clientDispatcher, getClientConfiguration(TCP_ADDRESS, 10000))) {
                assertNotNull(netconfClient2.getCapabilities());
            }
        }
    }

    @Test
    public void rpcReplyContainsAllAttributesTest() throws Exception {
        try (TestingNetconfClient netconfClient = createSession(TCP_ADDRESS, "1")) {
            final String rpc = "<rpc message-id=\"5\" a=\"a\" b=\"44\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"><get/>" + "</rpc>";
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
        try (TestingNetconfClient netconfClient = createSession(TCP_ADDRESS, "1")) {
            final String rpc = "<rpc message-id=\"1\" a=\"adada\" b=\"4\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"><commit/>" + "</rpc>";
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
        final ObjectName dep = transaction.createModule(DepTestImplModuleFactory.NAME, "instanceD");
        final ObjectName impl = transaction.createModule(NetconfTestImplModuleFactory.NAME, "instance");
        final NetconfTestImplModuleMXBean proxy = configRegistryClient
                .newMXBeanProxy(impl, NetconfTestImplModuleMXBean.class);
        proxy.setTestingDep(dep);
        proxy.setSimpleShort((short) 0);

        transaction.commit();

        try (TestingNetconfClient netconfClient = createSession(TCP_ADDRESS, "1")) {
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
        try (TestingNetconfClient netconfClient = createSession(TCP_ADDRESS, "1")) {

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
        try (TestingNetconfClient netconfClient = createSession(TCP_ADDRESS, "1")) {
            // send edit_config.xml
            final Document rpcReply = netconfClient.sendMessage(this.editConfig).getDocument();
            assertIsOK(rpcReply);
        }
    }

    @Test
    public void testValidate() throws Exception {
        try (TestingNetconfClient netconfClient = createSession(TCP_ADDRESS, "1")) {
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
        return assertGetConfigWorks(netconfClient, getGetConfig());
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
        try (TestingNetconfClient netconfClient = createSession(TCP_ADDRESS, "1")) {
            assertGetConfigWorks(netconfClient);
        }
    }

    @Test
    public void createYangTestBasedOnYuma() throws Exception {
        try (TestingNetconfClient netconfClient = createSession(TCP_ADDRESS, "1")) {
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
            final Set<ObjectName> cfgBeans = configRegistryClient.lookupConfigBeans();
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
        final NetconfMessage editId = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/editConfig_identities.xml");
        final NetconfMessage commit = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/commit.xml");

        try (TestingNetconfClient netconfClient = createSession(TCP_ADDRESS, "1")) {
            assertIsOK(netconfClient.sendMessage(editId).getDocument());
            assertIsOK(netconfClient.sendMessage(commit).getDocument());

            final NetconfMessage response = netconfClient.sendMessage(getGetConfig());

            assertThat(XmlUtil.toString(response.getDocument()), containsString("<afi xmlns:prefix=\"urn:opendaylight:params:xml:ns:yang:controller:config:test:types\">prefix:test-identity1</afi>"));
            assertThat(XmlUtil.toString(response.getDocument()), containsString("<afi xmlns:prefix=\"urn:opendaylight:params:xml:ns:yang:controller:config:test:types\">prefix:test-identity2</afi>"));
            assertThat(XmlUtil.toString(response.getDocument()), containsString("<safi xmlns:prefix=\"urn:opendaylight:params:xml:ns:yang:controller:config:test:types\">prefix:test-identity2</safi>"));
            assertThat(XmlUtil.toString(response.getDocument()), containsString("<safi xmlns:prefix=\"urn:opendaylight:params:xml:ns:yang:controller:config:test:types\">prefix:test-identity1</safi>"));

        } catch (final Exception e) {
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
        try (TestingNetconfClient netconfClient = createSession(TCP_ADDRESS, "1")) {
            final Document rpcReply = netconfClient.sendMessage(
                    XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/editConfig_merge_multiple-deps1.xml"))
                    .getDocument();
            assertIsOK(rpcReply);
            commit(netconfClient);
        }
        // verify that parent.getTestingDeps == d1,d2
        final MultipleDependenciesModuleMXBean parentProxy = configRegistryClient.newMXBeanProxy(
                configRegistryClient.lookupConfigBean(MultipleDependenciesModuleFactory.NAME, "parent"),
                MultipleDependenciesModuleMXBean.class);
        {
            final List<ObjectName> testingDeps = parentProxy.getTestingDeps();
            assertEquals(2, testingDeps.size());
            final Set<String> actualRefs = getServiceReferences(testingDeps);
            assertEquals(Sets.newHashSet("ref_d1", "ref_d2"), actualRefs);
        }

        // push second xml, should add d3 to parent's dependencies
        mergeD3(parentProxy);
        // push second xml again, to test that d3 is not added again
        mergeD3(parentProxy);
    }

    public void mergeD3(final MultipleDependenciesModuleMXBean parentProxy) throws Exception {
        try (TestingNetconfClient netconfClient = new TestingNetconfClient(
                "test " + TCP_ADDRESS.toString(), clientDispatcher, getClientConfiguration(TCP_ADDRESS, 5000))) {

            final Document rpcReply = netconfClient.sendMessage(
                    XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/editConfig_merge_multiple-deps2.xml"))
                    .getDocument();
            assertIsOK(rpcReply);
            commit(netconfClient);
        }
        {
            final List<ObjectName> testingDeps = parentProxy.getTestingDeps();
            assertEquals(3, testingDeps.size());
            final Set<String> actualRefs = getServiceReferences(testingDeps);
            assertEquals(Sets.newHashSet("ref_d1", "ref_d2", "ref_d3"), actualRefs);
        }
    }

    public Set<String> getServiceReferences(final List<ObjectName> testingDeps) {
        return new HashSet<>(Lists.transform(testingDeps, new Function<ObjectName, String>() {
            @Override
            public String apply(final ObjectName input) {
                return ObjectNameUtil.getReferenceName(input);
            }
        }));
    }

    public void commit(final TestingNetconfClient netconfClient) throws Exception {
        final Document rpcReply;
        rpcReply = netconfClient.sendMessage(XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/commit.xml"))
                .getDocument();
        assertIsOK(rpcReply);
    }
}
