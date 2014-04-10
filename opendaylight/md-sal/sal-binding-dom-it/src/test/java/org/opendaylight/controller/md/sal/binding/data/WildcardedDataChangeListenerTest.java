package org.opendaylight.controller.md.sal.binding.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.features.TableFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.features.TableFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.features.TableFeaturesKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.util.concurrent.SettableFuture;

public class WildcardedDataChangeListenerTest extends AbstractDataServiceTest {

    private static final NodeKey NODE_0_KEY = new NodeKey(new NodeId("test:0"));
    private static final NodeKey NODE_1_KEY = new NodeKey(new NodeId("test:1"));

    public static final InstanceIdentifier<Flow> DEEP_WILDCARDED_PATH = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class) //
            .augmentation(FlowCapableNode.class) //
            .child(Table.class) //
            .child(Flow.class) //
            .build();

    private static final TableKey TABLE_0_KEY = new TableKey((short) 0);
    private static final TableFeaturesKey TABLE_FEATURES_KEY = new TableFeaturesKey((short) 0);

    private static final InstanceIdentifier<Table> NODE_0_TABLE_PATH = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, NODE_0_KEY) //
            .augmentation(FlowCapableNode.class) //
            .child(Table.class, TABLE_0_KEY) //
            .build();

    private static final InstanceIdentifier<Table> NODE_1_TABLE_PATH = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, NODE_1_KEY) //
            .augmentation(FlowCapableNode.class) //
            .child(Table.class, TABLE_0_KEY) //
            .build();

    private static final FlowKey FLOW_KEY = new FlowKey(new FlowId("test"));

    private static final InstanceIdentifier<Flow> NODE_0_FLOW_PATH = NODE_0_TABLE_PATH.child(Flow.class, FLOW_KEY);

    private static final InstanceIdentifier<Flow> NODE_1_FLOW_PATH = NODE_1_TABLE_PATH.child(Flow.class, FLOW_KEY);

    private static final InstanceIdentifier<TableFeatures> NODE_0_TABLE_FEATURES_PATH = NODE_0_TABLE_PATH.child(
            TableFeatures.class, TABLE_FEATURES_KEY);

    private static final TableFeatures TABLE_FEATURES = new TableFeaturesBuilder()//
            .setKey(TABLE_FEATURES_KEY) //
            .setName("Foo") //
            .setMaxEntries(1000L) //
            .build();

    private static final Flow FLOW = new FlowBuilder() //
            .setKey(FLOW_KEY) //
            .setBarrier(true) //
            .setStrict(true) //
            .build();

    @Test
    public void test() throws InterruptedException, TimeoutException, ExecutionException {

        DataProviderService dataBroker = testContext.getBindingDataBroker();

        final SettableFuture<DataChangeEvent<InstanceIdentifier<?>, DataObject>> eventFuture = SettableFuture.create();
        dataBroker.registerDataChangeListener(DEEP_WILDCARDED_PATH, new DataChangeListener() {

            @Override
            public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent) {
                eventFuture.set(dataChangeEvent);
            }
        });

        DataModificationTransaction transaction = dataBroker.beginTransaction();
        transaction.putOperationalData(NODE_0_TABLE_FEATURES_PATH, TABLE_FEATURES);
        transaction.putOperationalData(NODE_0_FLOW_PATH, FLOW);
        transaction.putOperationalData(NODE_1_FLOW_PATH, FLOW);

        DataChangeEvent<InstanceIdentifier<?>, DataObject> event = eventFuture.get(1000, TimeUnit.MILLISECONDS);
        assertNotNull(event);
        assertTrue(event.getCreatedOperationalData().containsKey(NODE_0_FLOW_PATH));
        assertTrue(event.getCreatedOperationalData().containsKey(NODE_1_FLOW_PATH));
        assertFalse(event.getCreatedOperationalData().containsKey(NODE_0_TABLE_FEATURES_PATH));
    }

}
