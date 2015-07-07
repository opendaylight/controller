/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.notNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.JSONRestconfServiceImpl;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.common.OperationFailedException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Unit tests for JSONRestconfServiceImpl.
 *
 * @author Thomas Pantelis
 */
public class JSONRestconfServiceImplTest {
    static final String IETF_INTERFACES_NS = "urn:ietf:params:xml:ns:yang:ietf-interfaces";
    static final String IETF_INTERFACES_VERSION = "2013-07-04";
    static final QName INTERFACES_QNAME = QName.create(IETF_INTERFACES_NS, IETF_INTERFACES_VERSION, "interfaces");
    static final QName INTERFACE_QNAME = QName.create(IETF_INTERFACES_NS, IETF_INTERFACES_VERSION, "interface");
    static final QName NAME_QNAME = QName.create(IETF_INTERFACES_NS, IETF_INTERFACES_VERSION, "name");
    static final QName TYPE_QNAME = QName.create(IETF_INTERFACES_NS, IETF_INTERFACES_VERSION, "type");
    static final QName ENABLED_QNAME = QName.create(IETF_INTERFACES_NS, IETF_INTERFACES_VERSION, "enabled");
    static final QName DESC_QNAME = QName.create(IETF_INTERFACES_NS, IETF_INTERFACES_VERSION, "description");

    static final String TEST_MODULE_NS = "test:module";
    static final String TEST_MODULE_VERSION = "2014-01-09";
    static final QName TEST_CONT_QNAME = QName.create(TEST_MODULE_NS, TEST_MODULE_VERSION, "cont");
    static final QName TEST_CONT1_QNAME = QName.create(TEST_MODULE_NS, TEST_MODULE_VERSION, "cont1");
    static final QName TEST_LF11_QNAME = QName.create(TEST_MODULE_NS, TEST_MODULE_VERSION, "lf11");
    static final QName TEST_LF12_QNAME = QName.create(TEST_MODULE_NS, TEST_MODULE_VERSION, "lf12");

    private static BrokerFacade brokerFacade;

    private final JSONRestconfServiceImpl service = new JSONRestconfServiceImpl();

    @BeforeClass
    public static void init() throws IOException {
        ControllerContext.getInstance().setSchemas(TestUtils.loadSchemaContext("/full-versions/yangs"));
        brokerFacade = mock(BrokerFacade.class);
        RestconfImpl.getInstance().setBroker(brokerFacade);
        RestconfImpl.getInstance().setControllerContext(ControllerContext.getInstance());
    }

    @Before
    public void setup() {
        reset(brokerFacade);
    }

    private String loadData(String path) throws IOException {
        InputStream stream = JSONRestconfServiceImplTest.class.getResourceAsStream(path);
        return IOUtils.toString(stream, "UTF-8");
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testPut() throws Exception {
        doReturn(Futures.immediateCheckedFuture(null)).when(brokerFacade).commitConfigurationDataPut(
                notNull(SchemaContext.class), notNull(YangInstanceIdentifier.class), notNull(NormalizedNode.class));

        String uriPath = "/ietf-interfaces:interfaces/interface/eth0";
        String payload = loadData("/parts/ietf-interfaces_interfaces.json");

        service.put(uriPath, payload);

        ArgumentCaptor<YangInstanceIdentifier> capturedPath = ArgumentCaptor.forClass(YangInstanceIdentifier.class);
        ArgumentCaptor<NormalizedNode> capturedNode = ArgumentCaptor.forClass(NormalizedNode.class);
        verify(brokerFacade).commitConfigurationDataPut(notNull(SchemaContext.class), capturedPath.capture(),
                capturedNode.capture());

        verifyPath(capturedPath.getValue(), INTERFACES_QNAME, INTERFACE_QNAME,
                new Object[]{INTERFACE_QNAME, NAME_QNAME, "eth0"});

        assertTrue("Expected MapEntryNode. Actual " + capturedNode.getValue().getClass(),
                capturedNode.getValue() instanceof MapEntryNode);
        MapEntryNode actualNode = (MapEntryNode) capturedNode.getValue();
        assertEquals("MapEntryNode node type", INTERFACE_QNAME, actualNode.getNodeType());
        verifyLeafNode(actualNode, NAME_QNAME, "eth0");
        verifyLeafNode(actualNode, TYPE_QNAME, "ethernetCsmacd");
        verifyLeafNode(actualNode, ENABLED_QNAME, Boolean.FALSE);
        verifyLeafNode(actualNode, DESC_QNAME, "some interface");
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testPutBehindMountPoint() throws Exception {
        DOMMountPoint mockMountPoint = setupTestMountPoint();

        doReturn(Futures.immediateCheckedFuture(null)).when(brokerFacade).commitConfigurationDataPut(
                notNull(DOMMountPoint.class), notNull(YangInstanceIdentifier.class), notNull(NormalizedNode.class));

        String uriPath = "/ietf-interfaces:interfaces/yang-ext:mount/test-module:cont/cont1";
        String payload = loadData("/full-versions/testCont1Data.json");

        service.put(uriPath, payload);

        ArgumentCaptor<YangInstanceIdentifier> capturedPath = ArgumentCaptor.forClass(YangInstanceIdentifier.class);
        ArgumentCaptor<NormalizedNode> capturedNode = ArgumentCaptor.forClass(NormalizedNode.class);
        verify(brokerFacade).commitConfigurationDataPut(same(mockMountPoint), capturedPath.capture(),
                capturedNode.capture());

        verifyPath(capturedPath.getValue(), TEST_CONT_QNAME, TEST_CONT1_QNAME);

        assertTrue("Expected ContainerNode", capturedNode.getValue() instanceof ContainerNode);
        ContainerNode actualNode = (ContainerNode) capturedNode.getValue();
        assertEquals("ContainerNode node type", TEST_CONT1_QNAME, actualNode.getNodeType());
        verifyLeafNode(actualNode, TEST_LF11_QNAME, "lf11 data");
        verifyLeafNode(actualNode, TEST_LF12_QNAME, "lf12 data");
    }

    @Test(expected=TransactionCommitFailedException.class)
    public void testPutFailure() throws Throwable {
        doReturn(Futures.immediateFailedCheckedFuture(new TransactionCommitFailedException("mock")))
                .when(brokerFacade).commitConfigurationDataPut(notNull(SchemaContext.class),
                        notNull(YangInstanceIdentifier.class), notNull(NormalizedNode.class));

        String uriPath = "/ietf-interfaces:interfaces/interface/eth0";
        String payload = loadData("/parts/ietf-interfaces_interfaces.json");

        try {
            service.put(uriPath, payload);
        } catch (OperationFailedException e) {
            e.printStackTrace();
            assertNotNull(e.getCause());
            throw e.getCause();
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testPost() throws Exception {
        doReturn(Futures.immediateCheckedFuture(null)).when(brokerFacade).commitConfigurationDataPost(
                any(SchemaContext.class), any(YangInstanceIdentifier.class), any(NormalizedNode.class));

        String uriPath = null;
        String payload = loadData("/parts/ietf-interfaces_interfaces_absolute_path.json");

        service.post(uriPath, payload);

        ArgumentCaptor<YangInstanceIdentifier> capturedPath = ArgumentCaptor.forClass(YangInstanceIdentifier.class);
        ArgumentCaptor<NormalizedNode> capturedNode = ArgumentCaptor.forClass(NormalizedNode.class);
        verify(brokerFacade).commitConfigurationDataPost(notNull(SchemaContext.class), capturedPath.capture(),
                capturedNode.capture());

        verifyPath(capturedPath.getValue(), INTERFACES_QNAME);

        assertTrue("Expected ContainerNode", capturedNode.getValue() instanceof ContainerNode);
        ContainerNode actualNode = (ContainerNode) capturedNode.getValue();
        assertEquals("ContainerNode node type", INTERFACES_QNAME, actualNode.getNodeType());

        Optional<DataContainerChild<?, ?>> mapChild = actualNode.getChild(new NodeIdentifier(INTERFACE_QNAME));
        assertEquals(INTERFACE_QNAME.toString() + " present", true, mapChild.isPresent());
        assertTrue("Expected MapNode. Actual " + mapChild.get().getClass(), mapChild.get() instanceof MapNode);
        MapNode mapNode = (MapNode)mapChild.get();

        NodeIdentifierWithPredicates entryNodeID = new NodeIdentifierWithPredicates(
                INTERFACE_QNAME, NAME_QNAME, "eth0");
        Optional<MapEntryNode> entryChild = mapNode.getChild(entryNodeID);
        assertEquals(entryNodeID.toString() + " present", true, entryChild.isPresent());
        MapEntryNode entryNode = entryChild.get();
        verifyLeafNode(entryNode, NAME_QNAME, "eth0");
        verifyLeafNode(entryNode, TYPE_QNAME, "ethernetCsmacd");
        verifyLeafNode(entryNode, ENABLED_QNAME, Boolean.FALSE);
        verifyLeafNode(entryNode, DESC_QNAME, "some interface");
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testPostBehindMountPoint() throws Exception {
        DOMMountPoint mockMountPoint = setupTestMountPoint();

        doReturn(Futures.immediateCheckedFuture(null)).when(brokerFacade).commitConfigurationDataPost(
                notNull(DOMMountPoint.class), notNull(YangInstanceIdentifier.class), notNull(NormalizedNode.class));

        String uriPath = "/ietf-interfaces:interfaces/yang-ext:mount/test-module:cont";
        String payload = loadData("/full-versions/testCont1Data.json");

        service.post(uriPath, payload);

        ArgumentCaptor<YangInstanceIdentifier> capturedPath = ArgumentCaptor.forClass(YangInstanceIdentifier.class);
        ArgumentCaptor<NormalizedNode> capturedNode = ArgumentCaptor.forClass(NormalizedNode.class);
        verify(brokerFacade).commitConfigurationDataPost(same(mockMountPoint), capturedPath.capture(),
                capturedNode.capture());

        verifyPath(capturedPath.getValue(), TEST_CONT_QNAME, TEST_CONT1_QNAME);

        assertTrue("Expected ContainerNode", capturedNode.getValue() instanceof ContainerNode);
        ContainerNode actualNode = (ContainerNode) capturedNode.getValue();
        assertEquals("ContainerNode node type", TEST_CONT1_QNAME, actualNode.getNodeType());
        verifyLeafNode(actualNode, TEST_LF11_QNAME, "lf11 data");
        verifyLeafNode(actualNode, TEST_LF12_QNAME, "lf12 data");
    }

    @Test(expected=TransactionCommitFailedException.class)
    public void testPostFailure() throws Throwable {
        doReturn(Futures.immediateFailedCheckedFuture(new TransactionCommitFailedException("mock")))
                .when(brokerFacade).commitConfigurationDataPost(any(SchemaContext.class),
                        any(YangInstanceIdentifier.class), any(NormalizedNode.class));

        String uriPath = null;
        String payload = loadData("/parts/ietf-interfaces_interfaces_absolute_path.json");

        try {
            service.post(uriPath, payload);
        } catch (OperationFailedException e) {
            e.printStackTrace();
            assertNotNull(e.getCause());
            throw e.getCause();
        }
    }

    @Test
    public void testDelete() throws Exception {
        doReturn(Futures.immediateCheckedFuture(null)).when(brokerFacade).commitConfigurationDataDelete(
                notNull(YangInstanceIdentifier.class));

        String uriPath = "/ietf-interfaces:interfaces/interface/eth0";

        service.delete(uriPath);

        ArgumentCaptor<YangInstanceIdentifier> capturedPath = ArgumentCaptor.forClass(YangInstanceIdentifier.class);
        verify(brokerFacade).commitConfigurationDataDelete(capturedPath.capture());

        verifyPath(capturedPath.getValue(), INTERFACES_QNAME, INTERFACE_QNAME,
                new Object[]{INTERFACE_QNAME, NAME_QNAME, "eth0"});
    }

    @Test(expected=OperationFailedException.class)
    public void testDeleteFailure() throws Exception {
        String invalidUriPath = "/ietf-interfaces:interfaces/invalid";

        service.delete(invalidUriPath);
    }

    @Test
    public void testGetConfig() throws Exception {
        testGet(LogicalDatastoreType.CONFIGURATION);
    }

    @Test
    public void testGetOperational() throws Exception {
        testGet(LogicalDatastoreType.OPERATIONAL);
    }

    @Test
    public void testGetWithNoData() throws Exception {
        doReturn(null).when(brokerFacade).readConfigurationData(notNull(YangInstanceIdentifier.class));

        String uriPath = "/ietf-interfaces:interfaces";

        Optional<String> optionalResp = service.get(uriPath, LogicalDatastoreType.CONFIGURATION);

        assertEquals("Response present", false, optionalResp.isPresent());
    }

    @Test(expected=OperationFailedException.class)
    public void testGetFailure() throws Exception {
        String invalidUriPath = "/ietf-interfaces:interfaces/invalid";

        service.get(invalidUriPath, LogicalDatastoreType.CONFIGURATION);
    }

    void testGet(LogicalDatastoreType datastoreType) throws OperationFailedException {
        MapEntryNode entryNode = ImmutableNodes.mapEntryBuilder(INTERFACE_QNAME, NAME_QNAME, "eth0")
                .withChild(ImmutableNodes.leafNode(NAME_QNAME, "eth0"))
                .withChild(ImmutableNodes.leafNode(TYPE_QNAME, "ethernetCsmacd"))
                .withChild(ImmutableNodes.leafNode(ENABLED_QNAME, Boolean.TRUE))
                .withChild(ImmutableNodes.leafNode(DESC_QNAME, "eth interface"))
                .build();

        if(datastoreType == LogicalDatastoreType.CONFIGURATION) {
            doReturn(entryNode).when(brokerFacade).readConfigurationData(notNull(YangInstanceIdentifier.class));
        } else {
            doReturn(entryNode).when(brokerFacade).readOperationalData(notNull(YangInstanceIdentifier.class));
        }

        String uriPath = "/ietf-interfaces:interfaces/interface/eth0";

        Optional<String> optionalResp = service.get(uriPath, datastoreType);
        assertEquals("Response present", true, optionalResp.isPresent());
        String jsonResp = optionalResp.get();

        assertNotNull("Return null response", jsonResp);
        assertThat("Missing \"name\"", jsonResp, containsString("\"name\":\"eth0\""));
        assertThat("Missing \"type\"", jsonResp, containsString("\"type\":\"ethernetCsmacd\""));
        assertThat("Missing \"enabled\"", jsonResp, containsString("\"enabled\":true"));
        assertThat("Missing \"description\"", jsonResp, containsString("\"description\":\"eth interface\""));

        ArgumentCaptor<YangInstanceIdentifier> capturedPath = ArgumentCaptor.forClass(YangInstanceIdentifier.class);
        if (datastoreType == LogicalDatastoreType.CONFIGURATION) {
            verify(brokerFacade).readConfigurationData(capturedPath.capture());
        } else {
            verify(brokerFacade).readOperationalData(capturedPath.capture());
        }

        verifyPath(capturedPath.getValue(), INTERFACES_QNAME, INTERFACE_QNAME,
                new Object[]{INTERFACE_QNAME, NAME_QNAME, "eth0"});
    }

    DOMMountPoint setupTestMountPoint() throws FileNotFoundException {
        SchemaContext schemaContextTestModule = TestUtils.loadSchemaContext("/full-versions/test-module");
        DOMMountPoint mockMountPoint = mock(DOMMountPoint.class);
        doReturn(schemaContextTestModule).when(mockMountPoint).getSchemaContext();

        DOMMountPointService mockMountService = mock(DOMMountPointService.class);
        doReturn(Optional.of(mockMountPoint)).when(mockMountService).getMountPoint(notNull(YangInstanceIdentifier.class));

        ControllerContext.getInstance().setMountService(mockMountService);
        return mockMountPoint;
    }

    void verifyLeafNode(DataContainerNode<?> parent, QName leafType, Object leafValue) {
        Optional<DataContainerChild<?, ?>> leafChild = parent.getChild(new NodeIdentifier(leafType));
        assertEquals(leafType.toString() + " present", true, leafChild.isPresent());
        assertEquals(leafType.toString() + " value", leafValue, leafChild.get().getValue());
    }

    void verifyPath(YangInstanceIdentifier path, Object... expArgs) {
        List<PathArgument> pathArgs = path.getPathArguments();
        assertEquals("Arg count for actual path " + path, expArgs.length, pathArgs.size());
        int i = 0;
        for(PathArgument actual: pathArgs) {
            QName expNodeType;
            if(expArgs[i] instanceof Object[]) {
                Object[] listEntry = (Object[]) expArgs[i];
                expNodeType = (QName) listEntry[0];

                assertTrue(actual instanceof NodeIdentifierWithPredicates);
                Map<QName, Object> keyValues = ((NodeIdentifierWithPredicates)actual).getKeyValues();
                assertEquals(String.format("Path arg %d keyValues size", i + 1), 1, keyValues.size());
                QName expKey = (QName) listEntry[1];
                assertEquals(String.format("Path arg %d keyValue for %s", i + 1, expKey), listEntry[2],
                        keyValues.get(expKey));
            } else {
                expNodeType = (QName) expArgs[i];
            }

            assertEquals(String.format("Path arg %d node type", i + 1), expNodeType, actual.getNodeType());
            i++;
        }

    }
}
