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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.runtime.RootRuntimeBeanRegistrator;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.manager.impl.jmx.RootRuntimeBeanRegistratorImpl;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.store.api.YangStoreSnapshot;
import org.opendaylight.controller.config.yang.store.impl.MbeParser;
import org.opendaylight.controller.config.yang.test.impl.Asdf;
import org.opendaylight.controller.config.yang.test.impl.ComplexDtoBInner;
import org.opendaylight.controller.config.yang.test.impl.ComplexList;
import org.opendaylight.controller.config.yang.test.impl.Deep;
import org.opendaylight.controller.config.yang.test.impl.Deep2;
import org.opendaylight.controller.config.yang.test.impl.Deep3;
import org.opendaylight.controller.config.yang.test.impl.Deep4;
import org.opendaylight.controller.config.yang.test.impl.DepTestImplModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.DtoAInner;
import org.opendaylight.controller.config.yang.test.impl.DtoAInnerInner;
import org.opendaylight.controller.config.yang.test.impl.DtoC;
import org.opendaylight.controller.config.yang.test.impl.DtoD;
import org.opendaylight.controller.config.yang.test.impl.InnerInnerRunningDataRuntimeMXBean;
import org.opendaylight.controller.config.yang.test.impl.InnerRunningDataAdditionalRuntimeMXBean;
import org.opendaylight.controller.config.yang.test.impl.InnerRunningDataRuntimeMXBean;
import org.opendaylight.controller.config.yang.test.impl.InnerRunningDataRuntimeRegistration;
import org.opendaylight.controller.config.yang.test.impl.NetconfTestImplModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.NetconfTestImplModuleMXBean;
import org.opendaylight.controller.config.yang.test.impl.NetconfTestImplRuntimeMXBean;
import org.opendaylight.controller.config.yang.test.impl.NetconfTestImplRuntimeRegistration;
import org.opendaylight.controller.config.yang.test.impl.NetconfTestImplRuntimeRegistrator;
import org.opendaylight.controller.config.yang.test.impl.NotStateBean;
import org.opendaylight.controller.config.yang.test.impl.NotStateBeanInternal;
import org.opendaylight.controller.config.yang.test.impl.Peers;
import org.opendaylight.controller.config.yang.test.impl.RetValContainer;
import org.opendaylight.controller.config.yang.test.impl.RetValList;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;


public class NetconfMappingTest extends AbstractConfigTest {
    private static final Logger logger = LoggerFactory.getLogger(NetconfMappingTest.class);

    private static final String INSTANCE_NAME = "test1";
    private static final String NETCONF_SESSION_ID = "foo";
    private NetconfTestImplModuleFactory factory;
    private DepTestImplModuleFactory factory2;

    @Mock
    YangStoreSnapshot yangStoreSnapshot;
    @Mock
    NetconfOperationRouter netconfOperationRouter;

    private TransactionProvider transactionProvider;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(getMbes()).when(this.yangStoreSnapshot).getModuleMXBeanEntryMap();
        this.factory = new NetconfTestImplModuleFactory();
        this.factory2 = new DepTestImplModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.factory, this.factory2));

        transactionProvider = new TransactionProvider(this.configRegistryClient, NETCONF_SESSION_ID);
    }

    private ObjectName createModule(final String instanceName) throws InstanceAlreadyExistsException {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();

        final ObjectName on = transaction.createModule(this.factory.getImplementationName(), instanceName);
        final NetconfTestImplModuleMXBean mxBean = transaction.newMXBeanProxy(on, NetconfTestImplModuleMXBean.class);
        setModule(mxBean, transaction);

        transaction.commit();
        return on;
    }

    @Test
    public void testConfigNetconf() throws Exception {

        createModule(INSTANCE_NAME);

        edit("netconfMessages/editConfig.xml");
        checkBinaryLeafEdited(getConfigCandidate());


        // default-operation:none, should not affect binary leaf
        edit("netconfMessages/editConfig_none.xml");
        checkBinaryLeafEdited(getConfigCandidate());

        // check after edit
        commit();
        Element response = getConfigRunning();

        checkBinaryLeafEdited(response);
        checkTypeConfigAttribute(response);

        edit("netconfMessages/editConfig_remove.xml");

        commit();
        response = getConfigCandidate();
        final String responseFromCandidate = XmlUtil.toString(response).replaceAll("\\s+", "");
        // System.out.println(responseFromCandidate);
        response = getConfigRunning();
        final String responseFromRunning = XmlUtil.toString(response).replaceAll("\\s+", "");
        // System.out.println(responseFromRunning);
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
            assertThat(message,
                    JUnitMatchers
                            .containsString("Element simple-long-2 present multiple times with different namespaces"));
            assertThat(message,
                    JUnitMatchers.containsString("urn:opendaylight:params:xml:ns:yang:controller:test:impl"));
            assertThat(message,
                    JUnitMatchers
                            .containsString(XmlNetconfConstants.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG));
            throw e;
        }
    }

    @Test(expected = NetconfDocumentedException.class)
    public void testDifferentNamespaceInTO() throws Exception {
        try {
            edit("netconfMessages/namespaces/editConfig_differentNamespaceTO.xml");
        } catch (NetconfDocumentedException e) {
            String message = e.getMessage();
            assertThat(message, JUnitMatchers.containsString("Unrecognised elements"));
            assertThat(message, JUnitMatchers.containsString("simple-int2"));
            assertThat(message, JUnitMatchers.containsString("dto_d"));
            throw e;
        }
    }

    @Test(expected = NetconfDocumentedException.class)
    public void testSameAttrDifferentNamespacesList() throws Exception {
        try {
            edit("netconfMessages/namespaces/editConfig_sameAttrDifferentNamespacesList.xml");
        } catch (NetconfDocumentedException e) {
            String message = e.getMessage();
            assertThat(message,
                    JUnitMatchers.containsString("Element binaryLeaf present multiple times with different namespaces"));
            assertThat(message,
                    JUnitMatchers.containsString("urn:opendaylight:params:xml:ns:yang:controller:test:impl"));
            assertThat(message,
                    JUnitMatchers
                            .containsString(XmlNetconfConstants.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG));
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
                assertThat(e.getMessage(), JUnitMatchers.containsString("Unrecognised elements"));
                assertThat(e.getMessage(), JUnitMatchers.containsString("unknownAttribute"));
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
        final NodeList children = response.getElementsByTagName("binaryLeaf");
        assertEquals(3, children.getLength());
        final StringBuffer buf = new StringBuffer();
        for (int i = 0; i < 3; i++) {
            final Element e = (Element) children.item(i);
            buf.append(XmlElement.fromDomElement(e).getTextContent());
        }
        assertEquals("810", buf.toString());

    }

    private void checkTypeConfigAttribute(Element response) {

        XmlElement modulesElement = XmlElement.fromDomElement(response).getOnlyChildElement("data")
                .getOnlyChildElement("modules");

        XmlElement configAttributeType = null;
        for (XmlElement moduleElement : modulesElement.getChildElements("module")) {
            for (XmlElement type : moduleElement.getChildElements("type")) {
                if (type.getAttribute(XmlUtil.XMLNS_ATTRIBUTE_KEY).equals("") == false) {
                    configAttributeType = type;
                }
            }
        }

        assertEquals("configAttributeType", configAttributeType.getTextContent());
    }

    private Map<String, Map<String, ModuleMXBeanEntry>> getMbes() throws Exception {
        final List<InputStream> yangDependencies = getYangs();

        final Map<String, Map<String, ModuleMXBeanEntry>> mBeanEntries = Maps.newHashMap();
        mBeanEntries.putAll(new MbeParser().parseYangFiles(yangDependencies).getModuleMXBeanEntryMap());

        return mBeanEntries;
    }

    @Test
    public void testConfigNetconfRuntime() throws Exception {

        ModuleIdentifier id = new ModuleIdentifier(NetconfTestImplModuleFactory.NAME, "instance");
        RootRuntimeBeanRegistrator rootReg = new RootRuntimeBeanRegistratorImpl(internalJmxRegistrator, id);
        NetconfTestImplRuntimeRegistrator registrator = new NetconfTestImplRuntimeRegistrator(rootReg);

        NetconfTestImplRuntimeRegistration a = registerRoot(registrator);
        InnerRunningDataRuntimeRegistration reg = registerInner(a);
        registerInner2(reg);

        id = new ModuleIdentifier(NetconfTestImplModuleFactory.NAME, "instance2");
        rootReg = new RootRuntimeBeanRegistratorImpl(internalJmxRegistrator, id);
        registrator = new NetconfTestImplRuntimeRegistrator(rootReg);

        a = registerRoot(registrator);
        registerAdditional(a);
        registerAdditional(a);
        registerAdditional(a);
        registerAdditional(a);
        reg = registerInner(a);
        registerInner2(reg);
        reg = registerInner(a);
        registerInner2(reg);
        registerInner2(reg);
        reg = registerInner(a);
        registerInner2(reg);
        registerInner2(reg);
        registerInner2(reg);
        reg = registerInner(a);
        registerInner2(reg);
        registerInner2(reg);
        registerInner2(reg);
        registerInner2(reg);

        Element response = get();

        System.err.println(XmlUtil.toString(response));

        assertEquals(2, getElementsSize(response, "module"));
        assertEquals(2, getElementsSize(response, "asdf"));
        assertEquals(5, getElementsSize(response, "inner-running-data"));
        assertEquals(5, getElementsSize(response, "deep2"));
        assertEquals(11, getElementsSize(response, "inner-inner-running-data"));
        assertEquals(11, getElementsSize(response, "deep3"));
        assertEquals(11 * 2, getElementsSize(response, "list-of-strings"));
        assertEquals(4, getElementsSize(response, "inner-running-data-additional"));
        assertEquals(4, getElementsSize(response, "deep4"));
        // TODO assert keys

        RuntimeRpc netconf = new RuntimeRpc(yangStoreSnapshot, configRegistryClient, NETCONF_SESSION_ID);

        response = executeOp(netconf, "netconfMessages/rpc.xml");
        assertThat(XmlUtil.toString(response), JUnitMatchers.containsString("testarg1".toUpperCase()));

        response = executeOp(netconf, "netconfMessages/rpcInner.xml");
        assertThat(XmlUtil.toString(response), JUnitMatchers.containsString("ok"));

        response = executeOp(netconf, "netconfMessages/rpcInnerInner.xml");
        assertThat(XmlUtil.toString(response), JUnitMatchers.containsString("true"));

        response = executeOp(netconf, "netconfMessages/rpcInnerInner_complex_output.xml");
        assertThat(XmlUtil.toString(response), JUnitMatchers.containsString("1"));
        assertThat(XmlUtil.toString(response), JUnitMatchers.containsString("2"));
    }

    private Element get() throws NetconfDocumentedException, ParserConfigurationException, SAXException, IOException {
        Get getOp = new Get(yangStoreSnapshot, configRegistryClient, NETCONF_SESSION_ID);
        return executeOp(getOp, "netconfMessages/get.xml");
    }

    private int getElementsSize(Element response, String elementName) {
        return response.getElementsByTagName(elementName).getLength();
    }

    private Object registerAdditional(final NetconfTestImplRuntimeRegistration a) {
        class InnerRunningDataAdditionalRuntimeMXBeanTest implements InnerRunningDataAdditionalRuntimeMXBean {

            private final int simpleInt;
            private final String simpleString;

            public InnerRunningDataAdditionalRuntimeMXBeanTest(final int simpleInt, final String simpleString) {
                this.simpleInt = simpleInt;
                this.simpleString = simpleString;
            }

            @Override
            public Integer getSimpleInt3() {
                return this.simpleInt;
            }

            @Override
            public Deep4 getDeep4() {
                final Deep4 d = new Deep4();
                d.setBoool(false);
                return d;
            }

            @Override
            public String getSimpleString() {
                return this.simpleString;
            }

            @Override
            public void noArgInner() {
            }

        }

        final int simpleInt = counter++;
        return a.register(new InnerRunningDataAdditionalRuntimeMXBeanTest(simpleInt, "randomString_" + simpleInt));
    }

    private void registerInner2(final InnerRunningDataRuntimeRegistration reg) {
        class InnerInnerRunningDataRuntimeMXBeanTest implements InnerInnerRunningDataRuntimeMXBean {

            private final int simpleInt;

            public InnerInnerRunningDataRuntimeMXBeanTest(final int simpleInt) {
                this.simpleInt = simpleInt;
            }

            @Override
            public List<NotStateBean> getNotStateBean() {
                final NotStateBean notStateBean = new NotStateBean();
                final NotStateBeanInternal notStateBeanInternal = new NotStateBeanInternal();
                notStateBean.setNotStateBeanInternal(Lists.newArrayList(notStateBeanInternal));
                return Lists.newArrayList(notStateBean);
            }

            @Override
            public Integer getSimpleInt3() {
                return this.simpleInt;
            }

            @Override
            public Deep3 getDeep3() {
                return new Deep3();
            }

            @Override
            public List<String> getListOfStrings() {
                return Lists.newArrayList("l1", "l2");
            }

            @Override
            public List<RetValList> listOutput() {
                return Lists.newArrayList(new RetValList());
            }

            @Override
            public Boolean noArgInnerInner(Integer integer, Boolean aBoolean) {
                return aBoolean;
            }

            @Override
            public RetValContainer containerOutput() {
                return new RetValContainer();
            }

            @Override
            public List<String> leafListOutput() {
                return Lists.newArrayList("1", "2");
            }

        }

        reg.register(new InnerInnerRunningDataRuntimeMXBeanTest(counter++));

    }

    private static int counter = 1000;

    private InnerRunningDataRuntimeRegistration registerInner(final NetconfTestImplRuntimeRegistration a) {

        class InnerRunningDataRuntimeMXBeanTest implements InnerRunningDataRuntimeMXBean {

            private final int simpleInt;

            public InnerRunningDataRuntimeMXBeanTest(final int simpleInt) {
                this.simpleInt = simpleInt;
            }

            @Override
            public Integer getSimpleInt3() {
                return this.simpleInt;
            }

            @Override
            public Deep2 getDeep2() {
                return new Deep2();
            }

        }
        return a.register(new InnerRunningDataRuntimeMXBeanTest(counter++));
    }

    private NetconfTestImplRuntimeRegistration registerRoot(final NetconfTestImplRuntimeRegistrator registrator) {
        final NetconfTestImplRuntimeRegistration a = registrator.register(new NetconfTestImplRuntimeMXBean() {

            @Override
            public Long getCreatedSessions() {
                return 11L;
            }

            @Override
            public Asdf getAsdf() {
                final Asdf asdf = new Asdf();
                asdf.setSimpleInt(55);
                asdf.setSimpleString("asdf");
                return asdf;
            }

            @Override
            public String noArg(final String arg1) {
                return arg1.toUpperCase();
            }

        });
        return a;
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
                "/META-INF/yang/config-test.yang", "/META-INF/yang/config-test-impl.yang",
                "/META-INF/yang/ietf-inet-types.yang");
        final Collection<InputStream> yangDependencies = new ArrayList<>();
        for (String path : paths) {
            final InputStream is = Preconditions
                    .checkNotNull(getClass().getResourceAsStream(path), path + " not found");
            yangDependencies.add(is);
        }
        return Lists.newArrayList(yangDependencies);
    }

    private void setModule(final NetconfTestImplModuleMXBean mxBean, final ConfigTransactionJMXClient transaction)
            throws InstanceAlreadyExistsException {
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

        final ObjectName testingDepOn = transaction.createModule(this.factory2.getImplementationName(), "dep");
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
