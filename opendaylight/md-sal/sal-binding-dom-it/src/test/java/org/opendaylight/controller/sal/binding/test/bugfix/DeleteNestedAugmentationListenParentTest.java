package org.opendaylight.controller.sal.binding.test.bugfix;

import static org.junit.Assert.assertFalse;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DeleteNestedAugmentationListenParentTest extends AbstractDataServiceTest {

    private static final NodeKey NODE_KEY = new NodeKey(new NodeId("foo"));

    private static final TableKey TABLE_KEY = new TableKey((short) 0);

    private static final FlowKey FLOW_KEY = new FlowKey(new FlowId("100"));

    private static final InstanceIdentifier<FlowCapableNode> NODE_AUGMENT_PATH = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class,NODE_KEY)
            .augmentation(FlowCapableNode.class)
            .build();

    private static final InstanceIdentifier<Flow> FLOW_PATH = NODE_AUGMENT_PATH.builder()
            .child(Table.class,TABLE_KEY)
            .child(Flow.class,FLOW_KEY)
            .build();


    @Test
    public void deleteChildListenParent() throws InterruptedException, ExecutionException {
        DataModificationTransaction initTx = baDataService.beginTransaction();

        initTx.putOperationalData(FLOW_PATH, flow());
        initTx.commit().get();

        final SettableFuture<DataChangeEvent<InstanceIdentifier<?>, DataObject>> event = SettableFuture.create();

        baDataService.registerDataChangeListener(FLOW_PATH, new DataChangeListener() {

            @Override
            public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
                event.set(change);
            }
        });

        DataModificationTransaction deleteTx = baDataService.beginTransaction();
        deleteTx.removeOperationalData(FLOW_PATH.augmentation(FlowStatisticsData.class));
        deleteTx.commit().get();

        DataChangeEvent<InstanceIdentifier<?>, DataObject> receivedEvent = event.get();
        assertFalse(receivedEvent.getRemovedOperationalData().contains(NODE_AUGMENT_PATH));
    }

    private Flow flow() {
        FlowBuilder builder = new FlowBuilder()
            .setKey(FLOW_KEY)
            .addAugmentation(FlowStatisticsData.class,new FlowStatisticsDataBuilder()
                    .setFlowStatistics(new FlowStatisticsBuilder().build())
                    .build())
            .setBarrier(true)
            .setMatch(new MatchBuilder()
            .build())
        ;
        return builder.build();
    }

}