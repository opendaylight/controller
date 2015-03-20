package org.opendaylight.controller.sal.restconf.impl.test;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;
import javax.ws.rs.core.UriInfo;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

public class YangTest {

    private static SchemaContext context;

    private static Module rootModule;
    private static NotificationDefinition ntf1Def;
    private static RpcDefinition rpcSubmitDef;

    private static QNameModule rootModuleQname;
    private static QNameModule rpcSubmitModuleQName;
    private static QNameModule ntf1ModuleQName;

    private static DataTree dataTree;

    private static QName ntf1;
    private static QName ntf1Cont;
    private static QName ntf1Name;

    private static QName devices;
    private static QName device;
    private static QName devType;
    private static QName devSn;
    private static QName devData;
    private static QName devCategory;
    private static QName category;
    private static QName devDescription;
    private static QName devOther;

    private static QName ordList;
    private static QName simpleMap;
    private static QName ordListId;
    private static QName ordListVal;

    private static QName percentages;

    private static QName rpc2;
    private static QName submitInput;
    private static QName submitOutput;
    private static QName rpcId;
    private static QName rpcType;

    private static ControllerContext controllerContext;

    @BeforeClass
    public static void init() throws URISyntaxException, IOException, YangSyntaxErrorException {

        initSchemaContext();
        initQnames();
        initDataTree();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testRpc() {

        UriInfo uriInfo = mock(UriInfo.class);
        final RestconfImpl restconfImpl = RestconfImpl.getInstance();

        ContainerSchemaNode rpcContSchemaNode = (ContainerSchemaNode) rpcSubmitDef.getInput();
        ContainerNode container = createSubmitInputCont(785, "rpc_type_str", rpcContSchemaNode);

        DOMRpcResult result = new DefaultDOMRpcResult(null, Collections.EMPTY_LIST);
        CheckedFuture<DOMRpcResult, DOMRpcException> future = Futures.immediateCheckedFuture(result);

        BrokerFacade brokerFacade = mock(BrokerFacade.class);
        when(brokerFacade.invokeRpc(eq(rpcSubmitDef.getPath()), any(NormalizedNode.class))).thenReturn(future);

        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);

        final NormalizedNodeContext payload = new NormalizedNodeContext(new InstanceIdentifierContext(null,
                rpcSubmitDef, null, context), container);

        final NormalizedNodeContext output = restconfImpl.invokeRpc("yang_test:rpc2", payload, uriInfo);
        assertTrue(output != null);
        assertTrue(output.getData() == null);
    }

    private static void initSchemaContext() throws URISyntaxException, IOException, YangSyntaxErrorException {

        File resourceFile = new File(YangTest.class.getResource("/yang_test/yang_test.yang").toURI());
        File resourceDir = resourceFile.getParentFile();

        YangParserImpl parser = YangParserImpl.getInstance();
        context = parser.parseFile(resourceFile, resourceDir);
        controllerContext = ControllerContext.getInstance();
        controllerContext.setGlobalSchema(context);

        Set<Module> modules = context.getModules();
        for (Module module : modules) {
            if (module.getName().equals("yang_test")) {
                rootModule = module;
            }
        }

        rootModuleQname = rootModule.getQNameModule();

        ntf1 = QName.create(rootModuleQname, "ntf1");
        rpc2 = QName.create(rootModuleQname, "rpc2");

        rpcSubmitModuleQName = rpc2.getModule();
        ntf1ModuleQName = ntf1.getModule();

        Set<RpcDefinition> rpcs = rootModule.getRpcs();
        for (RpcDefinition rpcDefinition : rpcs) {
            if (rpcDefinition.getQName().equals(rpc2)) {
                rpcSubmitDef = rpcDefinition;
            }
        }

        Set<NotificationDefinition> notifications = rootModule.getNotifications();
        for (NotificationDefinition notificationDefinition : notifications) {
            if (notificationDefinition.getQName().equals(ntf1)) {
                ntf1Def = notificationDefinition;
            }
        }
    }

    private static void initQnames() {

        ntf1Cont = QName.create(ntf1ModuleQName, "ntf1_c1");
        ntf1Name = QName.create(ntf1ModuleQName, "ntf1_name");

        devices = QName.create(rootModuleQname, "devices");
        device = QName.create(rootModuleQname, "device");
        devType = QName.create(rootModuleQname, "dev_type");
        devSn = QName.create(rootModuleQname, "dev_sn");
        devData = QName.create(rootModuleQname, "dev_data");
        devCategory = QName.create(rootModuleQname, "dev_category");
        category = QName.create(rootModuleQname, "category");
        devDescription = QName.create(rootModuleQname, "dev_description");
        devOther = QName.create(rootModuleQname, "other");

        ordList = QName.create(rootModuleQname, "ord_list");
        simpleMap = QName.create(rootModuleQname, "simple_map");
        ordListId = QName.create(rootModuleQname, "ord_list_id");
        ordListVal = QName.create(rootModuleQname, "ord_list_val");

        percentages = QName.create(rootModuleQname, "percentages");

        submitInput = QName.create(rpcSubmitModuleQName, "input");
        submitOutput = QName.create(rpcSubmitModuleQName, "output");
        rpcId = QName.create(rpcSubmitModuleQName, "rpc2_id");
        rpcType = QName.create(rpcSubmitModuleQName, "rpc2_type");
    }

    private static void initDataTree() {

        dataTree = InMemoryDataTreeFactory.getInstance().create();
        dataTree.setSchemaContext(context);

        DataTreeModification initialDataTreeModification = dataTree.takeSnapshot().newModification();

        ContainerSchemaNode devicesContSchemaNode = (ContainerSchemaNode) rootModule.getDataChildByName(devices);
        ContainerNode devicesCont = createDevicesCont(devicesContSchemaNode);
        YangInstanceIdentifier path1 = YangInstanceIdentifier.of(devices);
        initialDataTreeModification.write(path1, devicesCont);

        LeafListSchemaNode percentageLeafListSchemaNode = (LeafListSchemaNode) rootModule
                .getDataChildByName(percentages);
        LeafSetNode<Integer> percentagesCont = createPercentLeafList(percentageLeafListSchemaNode);
        YangInstanceIdentifier path2 = YangInstanceIdentifier.of(percentages);
        initialDataTreeModification.write(path2, percentagesCont);

        ContainerSchemaNode simpleMapContSchemaNode = (ContainerSchemaNode) rootModule.getDataChildByName(ordList);
        ContainerNode simpleMapCont = createOrdListCont(simpleMapContSchemaNode);
        YangInstanceIdentifier path3 = YangInstanceIdentifier.of(ordList);
        initialDataTreeModification.write(path3, simpleMapCont);

        DataTreeCandidate writeCandidate = dataTree.prepare(initialDataTreeModification);
        dataTree.commit(writeCandidate);

        System.out.println(dataTree.toString());
    }

    private static ContainerNode createDevicesCont(ContainerSchemaNode container) {

        ListSchemaNode listSchemaNode = (ListSchemaNode) container.getDataChildByName(device);

        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> containerBldr = Builders
                .containerBuilder(container);

        MapNode map = createDevicesList(listSchemaNode);
        containerBldr.addChild(map);

        return containerBldr.build();
    }

    private static MapNode createDevicesList(ListSchemaNode listSchemaNode) {

        CollectionNodeBuilder<MapEntryNode, MapNode> mapBldr = Builders.mapBuilder(listSchemaNode);

        mapBldr.addChild(createDevicesListEntry("dev_type_1", 1234, "data1", listSchemaNode));
        mapBldr.addChild(createDevicesListEntry("dev_type_2", 1235, "data2", listSchemaNode));

        return mapBldr.build();
    }

    private static MapEntryNode createDevicesListEntry(String devTypeVal, int devSnVal, String devDataVal,
            ListSchemaNode listSchemaNode) {

        LeafNode<String> devTypeLeaf = ImmutableNodes.leafNode(devType, devTypeVal);
        LeafNode<Integer> devSnLeaf = ImmutableNodes.leafNode(devSn, devSnVal);
        LeafNode<String> devDataLeaf = ImmutableNodes.leafNode(devData, devDataVal);

        DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> mapEntryBldr = Builders
                .mapEntryBuilder(listSchemaNode);

        mapEntryBldr.addChild(devTypeLeaf);
        mapEntryBldr.addChild(devSnLeaf);
        mapEntryBldr.addChild(devDataLeaf);

        return mapEntryBldr.build();
    }

    private static LeafSetNode<Integer> createPercentLeafList(LeafListSchemaNode percentageLeafListSchemaNode) {

        ListNodeBuilder<Integer, LeafSetEntryNode<Integer>> leafSetBuilder = Builders
                .leafSetBuilder(percentageLeafListSchemaNode);

        leafSetBuilder.addChild(createPercentLeafListEntry(1, percentageLeafListSchemaNode));
        leafSetBuilder.addChild(createPercentLeafListEntry(20, percentageLeafListSchemaNode));
        leafSetBuilder.addChild(createPercentLeafListEntry(101, percentageLeafListSchemaNode));
        leafSetBuilder.addChild(createPercentLeafListEntry(-5, percentageLeafListSchemaNode));

        return leafSetBuilder.build();
    }

    private static LeafSetEntryNode<Integer> createPercentLeafListEntry(int percentage,
            LeafListSchemaNode percentageLeafListSchemaNode) {

        NormalizedNodeAttrBuilder<NodeWithValue, Integer, LeafSetEntryNode<Integer>> leafSetEntryBuilder = Builders
                .leafSetEntryBuilder(percentageLeafListSchemaNode);

        leafSetEntryBuilder.withValue(percentage);

        return leafSetEntryBuilder.build();
    }

    private static ContainerNode createOrdListCont(ContainerSchemaNode container) {

        ListSchemaNode listSchemaNode = (ListSchemaNode) container.getDataChildByName(simpleMap);

        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> containerBldr = Builders
                .containerBuilder(container);

        MapNode map = createSimpleMapList(listSchemaNode);
        containerBldr.addChild(map);

        return containerBldr.build();
    }

    private static MapNode createSimpleMapList(ListSchemaNode listSchemaNode) {

        CollectionNodeBuilder<MapEntryNode, MapNode> mapBldr = Builders.mapBuilder(listSchemaNode);

        mapBldr.addChild(createSimpleMapListEntry(2347, "val1", listSchemaNode));
        mapBldr.addChild(createSimpleMapListEntry(2345, "val2", listSchemaNode));
        mapBldr.addChild(createSimpleMapListEntry(2346, "val3", listSchemaNode));

        return mapBldr.build();
    }

    private static MapEntryNode createSimpleMapListEntry(int idVal, String valVal, ListSchemaNode listSchemaNode) {

        LeafNode<Integer> idLeaf = ImmutableNodes.leafNode(ordListId, idVal);
        LeafNode<String> valLeaf = ImmutableNodes.leafNode(ordListVal, valVal);

        DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> mapEntryBldr = Builders
                .mapEntryBuilder(listSchemaNode);

        mapEntryBldr.addChild(idLeaf);
        mapEntryBldr.addChild(valLeaf);

        return mapEntryBldr.build();
    }

    private static ContainerNode createntf1Cont(String ntf1NameVal, ContainerSchemaNode container) {

        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> containerBldr = Builders
                .containerBuilder(container);

        LeafNode<String> ntf1NameLeaf = ImmutableNodes.leafNode(ntf1Name, ntf1NameVal);

        containerBldr.addChild(ntf1NameLeaf);

        return containerBldr.build();
    }

    private static ContainerNode createSubmitInputCont(int rpcIdVal, String rpcTypeVal, ContainerSchemaNode container) {

        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> containerBldr = Builders
                .containerBuilder(container);

        LeafNode<Integer> rpcIdLeaf = ImmutableNodes.leafNode(rpcId, rpcIdVal);
        LeafNode<String> rpcTypeLeaf = ImmutableNodes.leafNode(rpcType, rpcTypeVal);

        containerBldr.addChild(rpcIdLeaf);
        containerBldr.addChild(rpcTypeLeaf);

        return containerBldr.build();
    }

    @Test
    public void test() {

    }
}
