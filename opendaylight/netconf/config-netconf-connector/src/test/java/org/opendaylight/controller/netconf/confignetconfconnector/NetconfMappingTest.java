/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.store.api.YangStoreSnapshot;
import org.opendaylight.controller.config.yang.store.impl.MbeParser;
import org.opendaylight.controller.config.yang.test.impl.ComplexDtoBInner;
import org.opendaylight.controller.config.yang.test.impl.ComplexList;
import org.opendaylight.controller.config.yang.test.impl.Deep;
import org.opendaylight.controller.config.yang.test.impl.DepTestImplModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.DtoAInner;
import org.opendaylight.controller.config.yang.test.impl.DtoAInnerInner;
import org.opendaylight.controller.config.yang.test.impl.DtoC;
import org.opendaylight.controller.config.yang.test.impl.DtoD;
import org.opendaylight.controller.config.yang.test.impl.IdentityTestModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.NetconfTestImplModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.NetconfTestImplModuleMXBean;
import org.opendaylight.controller.config.yang.test.impl.Peers;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfOperationRouter;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.Commit;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.DiscardChanges;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditConfig;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.get.Get;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.getconfig.GetConfig;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.runtimerpc.RuntimeRpc;
import org.opendaylight.controller.netconf.confignetconfconnector.transactions.TransactionProvider;
import org.opendaylight.controller.netconf.impl.mapping.operations.DefaultCloseSession;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.test.types.rev131127.TestIdentity1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.test.types.rev131127.TestIdentity2;
import org.opendaylight.yangtools.yang.data.impl.codec.CodecRegistry;
import org.opendaylight.yangtools.yang.data.impl.codec.IdentityCodec;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;


public class NetconfMappingTest extends AbstractConfigTest {
    private static final Logger logger = LoggerFactory.getLogger(NetconfMappingTest.class);

    private static final String INSTANCE_NAME = "instance-from-code";
    private static final String NETCONF_SESSION_ID = "foo";
    private NetconfTestImplModuleFactory factory;
    private DepTestImplModuleFactory factory2;
    private IdentityTestModuleFactory factory3;

    @Mock
    YangStoreSnapshot yangStoreSnapshot;
    @Mock
    NetconfOperationRouter netconfOperationRouter;

    private TransactionProvider transactionProvider;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(getMbes()).when(this.yangStoreSnapshot).getModuleMXBeanEntryMap();
        doReturn(getModules()).when(this.yangStoreSnapshot).getModules();

        this.factory = new NetconfTestImplModuleFactory();
        this.factory2 = new DepTestImplModuleFactory();
        this.factory3 = new IdentityTestModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.factory, this.factory2,
                this.factory3));

        transactionProvider = new TransactionProvider(this.configRegistryClient, NETCONF_SESSION_ID);
    }

    private ObjectName createModule(final String instanceName) throws InstanceAlreadyExistsException, InstanceNotFoundException, URISyntaxException, ValidationException, ConflictingVersionException {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();

        final ObjectName on = transaction.createModule(this.factory.getImplementationName(), instanceName);
        final NetconfTestImplModuleMXBean mxBean = transaction.newMXBeanProxy(on, NetconfTestImplModuleMXBean.class);
        setModule(mxBean, transaction, instanceName + "_dep");

        int i = 1;
        for (Class<? extends AbstractServiceInterface> sInterface : factory.getImplementedServiceIntefaces()) {
            ServiceInterfaceAnnotation annotation = sInterface.getAnnotation(ServiceInterfaceAnnotation.class);
            transaction.saveServiceReference(
                    transaction.getServiceInterfaceName(annotation.namespace(), annotation.localName()), "ref_from_code_to_" + instanceName + "_" + i++,
                    on);

        }
        transaction.commit();
        return on;
    }

    @Test
    public void testIdentityRefs() throws Exception {
        edit("netconfMessages/editConfig_identities.xml");

        commit();
        getConfigRunning();
    }

    @Override
    protected CodecRegistry getCodecRegistry() {
        IdentityCodec<?> idCodec = mock(IdentityCodec.class);
        doReturn(TestIdentity1.class).when(idCodec).deserialize(TestIdentity1.QNAME);
        doReturn(TestIdentity2.class).when(idCodec).deserialize(TestIdentity2.QNAME);

        CodecRegistry codecReg = super.getCodecRegistry();
        doReturn(idCodec).when(codecReg).getIdentityCodec();
        return codecReg;
    }

    @Test
    public void testServicePersistance() throws Exception {
        createModule(INSTANCE_NAME);

        edit("netconfMessages/editConfig.xml");
        Element config = getConfigCandidate();
        assertCorrectServiceNames(config, 6, "ref_test2", "user_to_instance_from_code", "ref_dep_user",
                "ref_dep_user_two", "ref_from_code_to_instance-from-code_dep_1",
                "ref_from_code_to_instance-from-code_1");


        edit("netconfMessages/editConfig_addServiceName.xml");
        config = getConfigCandidate();
        assertCorrectServiceNames(config, 7, "ref_test2", "user_to_instance_from_code", "ref_dep_user",
                "ref_dep_user_two", "ref_from_code_to_instance-from-code_dep_1",
                "ref_from_code_to_instance-from-code_1", "ref_dep_user_another");

        edit("netconfMessages/editConfig_addServiceNameOnTest.xml");
        config = getConfigCandidate();
        assertCorrectServiceNames(config, 7, "ref_test2", "user_to_instance_from_code", "ref_dep_user",
                "ref_dep_user_two", "ref_from_code_to_instance-from-code_dep_1",
                "ref_from_code_to_instance-from-code_1", "ref_dep_user_another");

        commit();
        config = getConfigRunning();
        assertCorrectRefNamesForDependencies(config);
        assertCorrectServiceNames(config, 7, "ref_test2", "user_to_instance_from_code", "ref_dep_user",
                "ref_dep_user_two", "ref_from_code_to_instance-from-code_dep_1",
                "ref_from_code_to_instance-from-code_1", "ref_dep_user_another");

        edit("netconfMessages/editConfig_replace_default.xml");
        config = getConfigCandidate();
        assertCorrectServiceNames(config, 2, "ref_dep", "ref_dep2");

        edit("netconfMessages/editConfig_remove.xml");
        config = getConfigCandidate();
        assertCorrectServiceNames(config, 0);

        commit();
        config = getConfigCandidate();
        assertCorrectServiceNames(config, 0);

    }

    private void assertCorrectRefNamesForDependencies(Element config) {
        NodeList modulesList = config.getElementsByTagName("modules");
        assertEquals(1, modulesList.getLength());

        Element modules = (Element) modulesList.item(0);

        String trimmedModules = XmlUtil.toString(modules).replaceAll("\\s", "");
        int defaultRefNameCount = StringUtils.countMatches(trimmedModules, "ref_dep2");
        int userRefNameCount = StringUtils.countMatches(trimmedModules, "ref_dep_user_two");

        assertEquals(0, defaultRefNameCount);
        assertEquals(2, userRefNameCount);
    }

    private void assertCorrectServiceNames(Element configCandidate, int servicesSize, String... refNames) {
        NodeList elements = configCandidate.getElementsByTagName("provider");
        assertEquals(servicesSize, elements.getLength());

        NodeList servicesList = configCandidate.getElementsByTagName("services");
        assertEquals(1, servicesList.getLength());

        Element services = (Element) servicesList.item(0);
        String trimmedServices = XmlUtil.toString(services).replaceAll("\\s", "");

        for (String s : refNames) {
            assertThat(trimmedServices, JUnitMatchers.containsString(s));
        }
    }

    @Test
    public void testConfigNetconfUnionTypes() throws Exception {

        createModule(INSTANCE_NAME);

        edit("netconfMessages/editConfig.xml");
        commit();
        Element response = getConfigRunning();
        String trimmedResponse = XmlUtil.toString(response).replaceAll("\\s", "");
        assertContainsString(trimmedResponse, "<ipxmlns=\"urn:opendaylight:params:xml:ns:yang:controller:test:impl\">0:0:0:0:0:0:0:1</ip>");
        assertContainsString(trimmedResponse, "<union-test-attrxmlns=\"urn:opendaylight:params:xml:ns:yang:controller:test:impl\">456</union-test-attr>");


        edit("netconfMessages/editConfig_setUnions.xml");
        commit();
        response = getConfigRunning();

        trimmedResponse = XmlUtil.toString(response).replaceAll("\\s", "");
        assertContainsString(trimmedResponse, "<ipxmlns=\"urn:opendaylight:params:xml:ns:yang:controller:test:impl\">127.1.2.3</ip>");
        assertContainsString(trimmedResponse, "<union-test-attrxmlns=\"urn:opendaylight:params:xml:ns:yang:controller:test:impl\">randomStringForUnion</union-test-attr>");

    }

    @Test
    public void testConfigNetconf() throws Exception {

        createModule(INSTANCE_NAME);

        edit("netconfMessages/editConfig.xml");
        Element configCandidate = getConfigCandidate();
        checkBinaryLeafEdited(configCandidate);


        // default-operation:none, should not affect binary leaf
        edit("netconfMessages/editConfig_none.xml");
        checkBinaryLeafEdited(getConfigCandidate());

        // check after edit
        commit();
        Element response = getConfigRunning();

        checkBinaryLeafEdited(response);
        checkTypeConfigAttribute(response);
        checkTypedefs(response);
        checkTestingDeps(response);
        checkEnum(response);
        checkBigDecimal(response);

        edit("netconfMessages/editConfig_remove.xml");

        commit();
        response = getConfigCandidate();
        final String responseFromCandidate = XmlUtil.toString(response).replaceAll("\\s+", "");
        response = getConfigRunning();
        final String responseFromRunning = XmlUtil.toString(response).replaceAll("\\s+", "");
        assertEquals(responseFromCandidate, responseFromRunning);

        final String expectedResult = XmlFileLoader.fileToString("netconfMessages/editConfig_expectedResult.xml")
                .replaceAll("\\s+", "");

        assertEquals(expectedResult, responseFromRunning);
        assertEquals(expectedResult, responseFromCandidate);

        edit("netconfMessages/editConfig_none.xml");
        doNothing().when(netconfOperationRouter).close();
        closeSession();
        verify(netconfOperationRouter).close();
        verifyNoMoreInteractions(netconfOperationRouter);
    }

    private void checkBigDecimal(Element response) {
        String responseTrimmed = XmlUtil.toString(response).replaceAll("\\s", "");

        assertContainsString(responseTrimmed, "<sleep-factorxmlns=\"urn:opendaylight:params:xml:ns:yang:controller:test:impl\">2.58</sleep-factor>");
        // Default
        assertContainsString(responseTrimmed, "<sleep-factorxmlns=\"urn:opendaylight:params:xml:ns:yang:controller:test:impl\">2.00</sleep-factor>");

    }

    private void closeSession() throws NetconfDocumentedException, ParserConfigurationException, SAXException,
            IOException {
        DefaultCloseSession closeOp = new DefaultCloseSession(NETCONF_SESSION_ID);
        executeOp(closeOp, "netconfMessages/closeSession.xml");
    }

    private void edit(String resource) throws ParserConfigurationException, SAXException, IOException,
            NetconfDocumentedException {
        EditConfig editOp = new EditConfig(yangStoreSnapshot, transactionProvider, configRegistryClient,
                NETCONF_SESSION_ID);
        executeOp(editOp, resource);
    }

    private void commit() throws ParserConfigurationException, SAXException, IOException, NetconfDocumentedException {
        Commit commitOp = new Commit(transactionProvider, configRegistryClient, NETCONF_SESSION_ID);
        executeOp(commitOp, "netconfMessages/commit.xml");
    }

    private Element getConfigCandidate() throws ParserConfigurationException, SAXException, IOException,
            NetconfDocumentedException {
        GetConfig getConfigOp = new GetConfig(yangStoreSnapshot, Optional.<String> absent(), transactionProvider,
                configRegistryClient, NETCONF_SESSION_ID);
        return executeOp(getConfigOp, "netconfMessages/getConfig_candidate.xml");
    }

    private Element getConfigRunning() throws ParserConfigurationException, SAXException, IOException,
            NetconfDocumentedException {
        GetConfig getConfigOp = new GetConfig(yangStoreSnapshot, Optional.<String> absent(), transactionProvider,
                configRegistryClient, NETCONF_SESSION_ID);
        return executeOp(getConfigOp, "netconfMessages/getConfig.xml");
    }

    @Ignore("second edit message corrupted")
    @Test(expected = NetconfDocumentedException.class)
    public void testConfigNetconfReplaceDefaultEx() throws Exception {

        createModule(INSTANCE_NAME);

        edit("netconfMessages/editConfig.xml");
        edit("netconfMessages/editConfig_replace_default_ex.xml");
    }

    @Test
    public void testConfigNetconfReplaceDefault() throws Exception {

        createModule(INSTANCE_NAME);

        edit("netconfMessages/editConfig.xml");
        commit();
        Element response = getConfigRunning();
        final int allInstances = response.getElementsByTagName("module").getLength();

        edit("netconfMessages/editConfig_replace_default.xml");

        commit();
        response = getConfigRunning();

        final int afterReplace = response.getElementsByTagName("module").getLength();
        assertEquals(4, allInstances);
        assertEquals(2, afterReplace);
    }

    @Test(expected = NetconfDocumentedException.class)
    public void testSameAttrDifferentNamespaces() throws Exception {
        try {
            edit("netconfMessages/namespaces/editConfig_sameAttrDifferentNamespaces.xml");
        } catch (NetconfDocumentedException e) {
            String message = e.getMessage();
            assertContainsString(message, "Element simple-long-2 present multiple times with different namespaces");
            assertContainsString(message, "urn:opendaylight:params:xml:ns:yang:controller:test:impl");
            assertContainsString(message, XmlNetconfConstants.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG);
            throw e;
        }
    }

    @Test(expected = NetconfDocumentedException.class)
    public void testDifferentNamespaceInTO() throws Exception {
        try {
            edit("netconfMessages/namespaces/editConfig_differentNamespaceTO.xml");
        } catch (NetconfDocumentedException e) {
            String message = e.getMessage();
            assertContainsString(message, "Unrecognised elements");
            assertContainsString(message, "simple-int2");
            assertContainsString(message, "dto_d");
            throw e;
        }
    }

    @Test(expected = NetconfDocumentedException.class)
    public void testSameAttrDifferentNamespacesList() throws Exception {
        try {
            edit("netconfMessages/namespaces/editConfig_sameAttrDifferentNamespacesList.xml");
        } catch (NetconfDocumentedException e) {
            String message = e.getMessage();
            assertContainsString(message, "Element binaryLeaf present multiple times with different namespaces");
            assertContainsString(message, "urn:opendaylight:params:xml:ns:yang:controller:test:impl");
            assertContainsString(message, XmlNetconfConstants.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG);
            throw e;
        }
    }

    @Test
    public void testTypeNameConfigAttributeMatching() throws Exception {
        edit("netconfMessages/editConfig.xml");
        commit();
        edit("netconfMessages/namespaces/editConfig_typeNameConfigAttributeMatching.xml");
        commit();

        Element response = getConfigRunning();
        checkTypeConfigAttribute(response);
    }

    // TODO add <modules operation="replace"> functionality
    @Test(expected = NetconfDocumentedException.class)
    public void testConfigNetconfReplaceModuleEx() throws Exception {

        createModule(INSTANCE_NAME);

        edit("netconfMessages/editConfig.xml");
        edit("netconfMessages/editConfig_replace_module_ex.xml");
    }

    @Test
    public void testUnrecognisedConfigElements() throws Exception {

        String format = "netconfMessages/unrecognised/editConfig_unrecognised%d.xml";
        final int TESTS_COUNT = 8;

        for (int i = 0; i < TESTS_COUNT; i++) {
            String file = String.format(format, i + 1);
            try {
                edit(file);
            } catch (NetconfDocumentedException e) {
                assertContainsString(e.getMessage(), "Unrecognised elements");
                assertContainsString(e.getMessage(), "unknownAttribute");
                continue;
            }
            fail("Unrecognised test should throw exception " + file);
        }
    }

    @Test
    @Ignore
    // FIXME
    public void testConfigNetconfReplaceModule() throws Exception {

        createModule(INSTANCE_NAME);

        edit("netconfMessages/editConfig.xml");
        commit();
        Element response = getConfigRunning();
        final int allInstances = response.getElementsByTagName("instance").getLength();

        edit("netconfMessages/editConfig_replace_module.xml");

        commit();
        response = getConfigRunning();
        final int afterReplace = response.getElementsByTagName("instance").getLength();

        assertEquals(4 + 4 /* Instances from services */, allInstances);
        assertEquals(3 + 3, afterReplace);
    }

    @Test(expected = NetconfDocumentedException.class)
    public void testEx() throws Exception {

        commit();
    }

    @Test(expected = NetconfDocumentedException.class)
    public void testEx2() throws Exception {
        discard();
    }

    private void discard() throws ParserConfigurationException, SAXException, IOException, NetconfDocumentedException {
        DiscardChanges discardOp = new DiscardChanges(transactionProvider, configRegistryClient, NETCONF_SESSION_ID);
        executeOp(discardOp, "netconfMessages/discardChanges.xml");
    }

    private void checkBinaryLeafEdited(final Element response) {
        String responseTrimmed = XmlUtil.toString(response).replaceAll("\\s", "");
        String substring = "<binaryLeafxmlns=\"urn:opendaylight:params:xml:ns:yang:controller:test:impl\">YmluYXJ5</binaryLeaf>";
        assertContainsString(responseTrimmed, substring);
        substring = "<binaryLeafxmlns=\"urn:opendaylight:params:xml:ns:yang:controller:test:impl\">ZGVmYXVsdEJpbg==</binaryLeaf>";
        assertContainsString(responseTrimmed, substring);
    }

    private void checkTypedefs(final Element response) {
        String responseTrimmed = XmlUtil.toString(response).replaceAll("\\s", "");

        String substring = "<extendedxmlns=\"urn:opendaylight:params:xml:ns:yang:controller:test:impl\">10</extended>";
        assertContainsString(responseTrimmed, substring);
        // Default
        assertContainsString(responseTrimmed,
                "<extendedxmlns=\"urn:opendaylight:params:xml:ns:yang:controller:test:impl\">1</extended>");

        assertContainsString(responseTrimmed,
                "<extended-twicexmlns=\"urn:opendaylight:params:xml:ns:yang:controller:test:impl\">20</extended-twice>");
        // Default
        assertContainsString(responseTrimmed,
                "<extended-twicexmlns=\"urn:opendaylight:params:xml:ns:yang:controller:test:impl\">2</extended-twice>");

        assertContainsString(responseTrimmed,
                "<extended-enumxmlns=\"urn:opendaylight:params:xml:ns:yang:controller:test:impl\">TWO</extended-enum>");
        // Default
        assertContainsString(responseTrimmed,
                "<extended-enumxmlns=\"urn:opendaylight:params:xml:ns:yang:controller:test:impl\">ONE</extended-enum>");
    }

    private void assertContainsString(String string, String substring) {
        assertThat(string, JUnitMatchers.containsString(substring));
    }

    private void checkEnum(final Element response) {
        XmlElement modulesElement = XmlElement.fromDomElement(response).getOnlyChildElement("data")
                .getOnlyChildElement("modules");

        String enumName = "extended-enum";
        String enumContent = "TWO";

        for (XmlElement moduleElement : modulesElement.getChildElements("module")) {
            String name = moduleElement.getOnlyChildElement("name").getTextContent();
            if(name.equals(INSTANCE_NAME)) {
                XmlElement enumAttr = moduleElement.getOnlyChildElement(enumName);
                assertEquals(enumContent, enumAttr.getTextContent());

                return;
            }
        }

        fail("Enum attribute " + enumName + ":" + enumContent + " not present in " + XmlUtil.toString(response));
    }

    private void checkTestingDeps(Element response) {
        int testingDepsSize = response.getElementsByTagName("testing-deps").getLength();
        assertEquals(2, testingDepsSize);
    }

    private void checkTypeConfigAttribute(Element response) {

        XmlElement modulesElement = XmlElement.fromDomElement(response).getOnlyChildElement("data")
                .getOnlyChildElement("modules");

        List<String> expectedValues = Lists.newArrayList("default-string", "configAttributeType");
        Set<String> configAttributeType = Sets.newHashSet();

        for (XmlElement moduleElement : modulesElement.getChildElements("module")) {
            for (XmlElement type : moduleElement.getChildElements("type")) {
                if (type.getAttribute(XmlUtil.XMLNS_ATTRIBUTE_KEY).equals("") == false) {
                    configAttributeType.add(type.getTextContent());
                }
            }
        }

        for (String expectedValue : expectedValues) {
            assertTrue(configAttributeType.contains(expectedValue));
        }
    }

    private Map<String, Map<String, ModuleMXBeanEntry>> getMbes() throws Exception {
        final List<InputStream> yangDependencies = getYangs();

        final Map<String, Map<String, ModuleMXBeanEntry>> mBeanEntries = Maps.newHashMap();
        mBeanEntries.putAll(new MbeParser().parseYangFiles(yangDependencies).getModuleMXBeanEntryMap());

        return mBeanEntries;
    }

    private Set<org.opendaylight.yangtools.yang.model.api.Module> getModules() throws Exception {
        SchemaContext resolveSchemaContext = getSchemaContext();
        return resolveSchemaContext.getModules();
    }

    private SchemaContext getSchemaContext() throws Exception {
        final List<InputStream> yangDependencies = getYangs();
        YangParserImpl parser = new YangParserImpl();

        Set<Module> allYangModules = parser.parseYangModelsFromStreams(yangDependencies);

        return parser.resolveSchemaContext(Sets
                .newHashSet(allYangModules));
    }

    @Test
    public void testConfigNetconfRuntime() throws Exception {

        createModule(INSTANCE_NAME);

        edit("netconfMessages/editConfig.xml");
        checkBinaryLeafEdited(getConfigCandidate());

        // check after edit
        commit();
        Element response = get();

        assertEquals(2/*With runtime beans*/ + 2 /*Without runtime beans*/, getElementsSize(response, "module"));
        // data from state
        assertEquals(2, getElementsSize(response, "asdf"));
        // data from running config
        assertEquals(2, getElementsSize(response, "simple-short"));

        assertEquals(8, getElementsSize(response, "inner-running-data"));
        assertEquals(8, getElementsSize(response, "deep2"));
        assertEquals(8 * 4, getElementsSize(response, "inner-inner-running-data"));
        assertEquals(8 * 4, getElementsSize(response, "deep3"));
        assertEquals(8 * 4 * 2, getElementsSize(response, "list-of-strings"));
        assertEquals(8, getElementsSize(response, "inner-running-data-additional"));
        assertEquals(8, getElementsSize(response, "deep4"));
        // TODO assert keys

        RuntimeRpc netconf = new RuntimeRpc(yangStoreSnapshot, configRegistryClient, NETCONF_SESSION_ID);

        response = executeOp(netconf, "netconfMessages/rpc.xml");
        assertContainsString(XmlUtil.toString(response), "testarg1".toUpperCase());

        response = executeOp(netconf, "netconfMessages/rpcInner.xml");
        assertContainsString(XmlUtil.toString(response), "ok");

        response = executeOp(netconf, "netconfMessages/rpcInnerInner.xml");
        assertContainsString(XmlUtil.toString(response), "true");

        response = executeOp(netconf, "netconfMessages/rpcInnerInner_complex_output.xml");
        assertContainsString(XmlUtil.toString(response), "1");
        assertContainsString(XmlUtil.toString(response), "2");
    }

    private Element get() throws NetconfDocumentedException, ParserConfigurationException, SAXException, IOException {
        Get getOp = new Get(yangStoreSnapshot, configRegistryClient, NETCONF_SESSION_ID, transactionProvider);
        return executeOp(getOp, "netconfMessages/get.xml");
    }

    private int getElementsSize(Element response, String elementName) {
        return response.getElementsByTagName(elementName).getLength();
    }

    private Element executeOp(final NetconfOperation op, final String filename) throws ParserConfigurationException,
            SAXException, IOException, NetconfDocumentedException {

        final Document request = XmlFileLoader.xmlFileToDocument(filename);

        logger.debug("Executing netconf operation\n{}", XmlUtil.toString(request));
        HandlingPriority priority = op.canHandle(request);

        Preconditions.checkState(priority != HandlingPriority.CANNOT_HANDLE);

        final Document response = op.handle(request, netconfOperationRouter);
        logger.debug("Got response\n{}", XmlUtil.toString(response));
        return response.getDocumentElement();
    }

    private List<InputStream> getYangs() throws FileNotFoundException {
        List<String> paths = Arrays.asList("/META-INF/yang/config.yang", "/META-INF/yang/rpc-context.yang",
                "/META-INF/yang/config-test.yang", "/META-INF/yang/config-test-impl.yang", "/META-INF/yang/test-types.yang",
                "/META-INF/yang/ietf-inet-types.yang");
        final Collection<InputStream> yangDependencies = new ArrayList<>();
        for (String path : paths) {
            final InputStream is = Preconditions
                    .checkNotNull(getClass().getResourceAsStream(path), path + " not found");
            yangDependencies.add(is);
        }
        return Lists.newArrayList(yangDependencies);
    }

    private void setModule(final NetconfTestImplModuleMXBean mxBean, final ConfigTransactionJMXClient transaction, String depName)
            throws InstanceAlreadyExistsException, InstanceNotFoundException {
        mxBean.setSimpleInt((long) 44);
        mxBean.setBinaryLeaf(new byte[] { 8, 7, 9 });
        final DtoD dtob = getDtoD();
        mxBean.setDtoD(dtob);
        //
        final DtoC dtoa = getDtoC();
        mxBean.setDtoC(dtoa);
        mxBean.setSimpleBoolean(false);
        //
        final Peers p1 = new Peers();
        p1.setCoreSize(44L);
        p1.setPort("port1");
        p1.setSimpleInt3(456);
        final Peers p2 = new Peers();
        p2.setCoreSize(44L);
        p2.setPort("port23");
        p2.setSimpleInt3(456);
        mxBean.setPeers(Lists.<Peers> newArrayList(p1, p2));
        // //
        mxBean.setSimpleLong(454545L);
        mxBean.setSimpleLong2(44L);
        mxBean.setSimpleBigInteger(BigInteger.valueOf(999L));
        mxBean.setSimpleByte(new Byte((byte) 4));
        mxBean.setSimpleShort(new Short((short) 4));
        mxBean.setSimpleTest(545);

        mxBean.setComplexList(Lists.<ComplexList> newArrayList());
        mxBean.setSimpleList(Lists.<Integer> newArrayList());

        final ObjectName testingDepOn = transaction.createModule(this.factory2.getImplementationName(), depName);
        int i = 1;
        for (Class<? extends AbstractServiceInterface> sInterface : factory2.getImplementedServiceIntefaces()) {
            ServiceInterfaceAnnotation annotation = sInterface.getAnnotation(ServiceInterfaceAnnotation.class);
            transaction.saveServiceReference(
                    transaction.getServiceInterfaceName(annotation.namespace(), annotation.localName()), "ref_from_code_to_" + depName + "_" + i++,
                    testingDepOn);

        }
        mxBean.setTestingDep(testingDepOn);
    }

    private static DtoD getDtoD() {
        final DtoD dtob = new DtoD();
        dtob.setSimpleInt1((long) 444);
        dtob.setSimpleInt2((long) 4444);
        dtob.setSimpleInt3(454);
        final ComplexDtoBInner dtobInner = new ComplexDtoBInner();
        final Deep deep = new Deep();
        deep.setSimpleInt3(4);
        dtobInner.setDeep(deep);
        dtobInner.setSimpleInt3(44);
        dtobInner.setSimpleList(Lists.newArrayList(4));
        dtob.setComplexDtoBInner(Lists.newArrayList(dtobInner));
        dtob.setSimpleList(Lists.newArrayList(4));
        return dtob;
    }

    private static DtoC getDtoC() {
        final DtoC dtoa = new DtoC();
        // dtoa.setSimpleArg((long) 55);
        final DtoAInner dtoAInner = new DtoAInner();
        final DtoAInnerInner dtoAInnerInner = new DtoAInnerInner();
        dtoAInnerInner.setSimpleArg(456L);
        dtoAInner.setDtoAInnerInner(dtoAInnerInner);
        dtoAInner.setSimpleArg(44L);
        dtoa.setDtoAInner(dtoAInner);
        return dtoa;
    }

}
