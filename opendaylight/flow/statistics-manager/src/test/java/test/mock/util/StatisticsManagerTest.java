package test.mock.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.md.statistics.manager.impl.StatisticsManagerConfig;
import org.opendaylight.controller.md.statistics.manager.impl.StatisticsManagerImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FeatureCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.flow.node.SwitchFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.port.mod.port.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemovedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.MeterFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class StatisticsManagerTest extends AbstractDataBrokerTest {

    public static final Counter64 COUNTER_64_TEST_VALUE = new Counter64(BigInteger.valueOf(128));
    public static final Counter32 COUNTER_32_TEST_VALUE = new Counter32(64L);
    public static final Long MAX_GROUPS_TEST_VALUE = 2000L;
    public static final BigInteger BIG_INTEGER_TEST_VALUE = BigInteger.valueOf(1000);

    private static final int DEFAULT_MIN_REQUEST_NET_MONITOR_INTERVAL = 5000;
    private static final int MAX_NODES_FOR_COLLECTOR = 16;

    private static Flow flow;
    private static Group group;
    private static Meter meter;
    private static Port port;
    private static Queue queue;
    private static TableId tableId;
    private static NodeConnectorId nodeConnectorId;

    private final NotificationProviderServiceHelper notificationMock = new NotificationProviderServiceHelper();
    protected final NodeKey s1Key = new NodeKey(new NodeId("S1"));
    protected RpcProviderRegistryMock rpcRegistry;

    @BeforeClass
    public static void setupTests() {
        flow = FlowMockGenerator.getRandomFlow();
        group = GroupMockGenerator.getRandomGroup();
        meter = MeterMockGenerator.getRandomMeter();
        port = PortMockGenerator.getRandomPort();
        queue = QueueMockGenerator.getRandomQueueWithPortNum(port.getPortNumber().getUint32());
        tableId = new TableId((short) 2);
        nodeConnectorId = new NodeConnectorId("connector.1");
    }

    @Before
    public void init() {
        rpcRegistry = new RpcProviderRegistryMock(notificationMock);
    }

    // node with statistics capabilities will enable cyclic statistics collection
    @SafeVarargs
    protected final void addFlowCapableNodeWithFeatures(final NodeKey nodeKey, final Boolean hasMeterCapabilities,
                                                     final Class<? extends FeatureCapability>... capabilities)
            throws ExecutionException, InterruptedException {
        final Nodes nodes = new NodesBuilder().setNode(Collections.<Node>emptyList()).build();
        final InstanceIdentifier<Node> flowNodeIdentifier = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, nodeKey);

        final FlowCapableNodeBuilder fcnBuilder = new FlowCapableNodeBuilder();
        final SwitchFeaturesBuilder sfBuilder = new SwitchFeaturesBuilder();
        final List<Class<? extends FeatureCapability>> capabilitiyList = new ArrayList<>();
        for (final Class<? extends FeatureCapability> capability : capabilities) {
            capabilitiyList.add(capability);
        }
        sfBuilder.setCapabilities(capabilitiyList);
        sfBuilder.setMaxTables((short) 255);
        final NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setKey(nodeKey);
        fcnBuilder.setSwitchFeatures(sfBuilder.build());
        final List<Table> tables = new ArrayList<>();
        final TableBuilder tBuilder = new TableBuilder();
        tBuilder.setId(getFlow().getTableId());
        tables.add(tBuilder.build());
        fcnBuilder.setTable(tables);
        final FlowCapableNode flowCapableNode = fcnBuilder.build();
        nodeBuilder.addAugmentation(FlowCapableNode.class, flowCapableNode);
        final Node node = nodeBuilder.build();

        final WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class), nodes);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, flowNodeIdentifier, nodeBuilder.build());
        if (hasMeterCapabilities) {
            final NodeMeterFeaturesBuilder nmfBuilder = new NodeMeterFeaturesBuilder();
            final MeterFeaturesBuilder mfBuilder = new MeterFeaturesBuilder();
            mfBuilder.setMaxBands((short) 4);
            nmfBuilder.setMeterFeatures(mfBuilder.build());
            writeTx.put(LogicalDatastoreType.OPERATIONAL, flowNodeIdentifier.augmentation(NodeMeterFeatures.class),
                    nmfBuilder.build());
        }
        writeTx.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(Nodes.class), nodes);
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowNodeIdentifier, node);
        assertCommit(writeTx.submit());

        final NodeUpdatedBuilder nuBuilder = new NodeUpdatedBuilder(node);
        final FlowCapableNodeUpdatedBuilder fcnuBuilder = new FlowCapableNodeUpdatedBuilder(flowCapableNode);
        nuBuilder.setNodeRef(new NodeRef(flowNodeIdentifier));
        nuBuilder.addAugmentation(FlowCapableNodeUpdated.class, fcnuBuilder.build());
        notificationMock.pushNotification(nuBuilder.build());
    }

    public void addFlowCapableNode(final NodeKey nodeKey) throws ExecutionException, InterruptedException {
        final Nodes nodes = new NodesBuilder().setNode(Collections.<Node>emptyList()).build();
        final InstanceIdentifier<Node> flowNodeIdentifier = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, nodeKey);

        final FlowCapableNodeBuilder fcnBuilder = new FlowCapableNodeBuilder();
        final NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setKey(nodeKey);
        final SwitchFeaturesBuilder sfBuilder = new SwitchFeaturesBuilder();
        sfBuilder.setMaxTables((short) 255);
        fcnBuilder.setSwitchFeatures(sfBuilder.build());
        final FlowCapableNode flowCapableNode = fcnBuilder.build();
        nodeBuilder.addAugmentation(FlowCapableNode.class, flowCapableNode);
        final Node node = nodeBuilder.build();

        final WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class), nodes);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, flowNodeIdentifier, node);
        writeTx.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(Nodes.class), nodes);
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowNodeIdentifier, node);
        assertCommit(writeTx.submit());

        final NodeUpdatedBuilder nuBuilder = new NodeUpdatedBuilder(node);
        final FlowCapableNodeUpdatedBuilder fcnuBuilder = new FlowCapableNodeUpdatedBuilder(flowCapableNode);
        nuBuilder.setNodeRef(new NodeRef(flowNodeIdentifier));
        nuBuilder.addAugmentation(FlowCapableNodeUpdated.class, fcnuBuilder.build());
        notificationMock.pushNotification(nuBuilder.build());
    }

    protected void removeNode(final NodeKey nodeKey) throws ExecutionException, InterruptedException {
        final InstanceIdentifier<Node> nodeII = InstanceIdentifier.create(Nodes.class).child(Node.class, nodeKey);

        final WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.OPERATIONAL, nodeII);
        writeTx.submit().get();

        final NodeRemovedBuilder nrBuilder = new NodeRemovedBuilder();
        nrBuilder.setNodeRef(new NodeRef(nodeII));
        notificationMock.pushNotification(nrBuilder.build());
    }

    public StatisticsManager setupStatisticsManager() {
        StatisticsManagerConfig.StatisticsManagerConfigBuilder confBuilder = StatisticsManagerConfig.builder();
        confBuilder.setMaxNodesForCollector(MAX_NODES_FOR_COLLECTOR);
        confBuilder.setMinRequestNetMonitorInterval(DEFAULT_MIN_REQUEST_NET_MONITOR_INTERVAL);
        StatisticsManager statsProvider = new StatisticsManagerImpl(getDataBroker(), confBuilder.build());
        statsProvider.start(notificationMock.getNotifBroker(), rpcRegistry);
        return statsProvider;
    }

    public static Flow getFlow() {
        return flow;
    }

    public static Group getGroup() {
        return group;
    }

    public static Meter getMeter() {
        return meter;
    }

    public static Port getPort() {
        return port;
    }

    public static Queue getQueue() {
        return queue;
    }

    public static TableId getTableId() {
        return tableId;
    }

    public static NodeConnectorId getNodeConnectorId() {
        return nodeConnectorId;
    }
}

