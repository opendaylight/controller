package org.opendaylight.pingDataListener;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.opendaylight.samplenodeextension.rev140402.SampleTestNode;

public class pingDataChangeListener implements DataChangeListener{

    private static final Logger LOG = LoggerFactory.getLogger(pingDataChangeListener.class);
    private DataBroker dataService;

    //public void setDataProviderService(DataProviderService dataService) {
    public void setDataProviderService(DataBroker dataService) {
        this.dataService = dataService;
    }


    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        System.out.println("$$$$$$$$$$$$$$$$$$ onDataChanged entered $$$$$$$$$$$$$$$$$$$$$");
        DataObject node = change.getUpdatedSubtree();
        if(node != null) {
            System.out.println(node.getClass().getSimpleName());
        }
        else {
            System.out.println("$$$$$$$$$$$$$$$node is NULL$$$$$$$$$$$$$$$$$$$$");
        }

        Map<InstanceIdentifier<?>, DataObject> created = change.getCreatedData();

        if(created.isEmpty()) {
            System.out.println("created map of objects is empty");
        }

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : created.entrySet())
        {
            DataObject node1 = entry.getValue();
            if(node1 != null) {
                System.out.println("Node1 is .................");
                System.out.println(node1.getClass().getSimpleName());
                node = node1;
            }
            else {
                System.out.println("$$$$$$$$$$$$$$$node1 is NULL$$$$$$$$$$$$$$$$$$$$");
            }
        }




        if(node instanceof FlowCapableNode) {
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
            System.out.println("$$$$$$$$$$$FlowCapableNode found.....$$$$$$$$$$$$$$$$$");
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        }
        if(node instanceof Node) {
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
            System.out.println("$$$$$$$$$$$Node found.....$$$$$$$$$$$$$$$$$");
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
            SampleTestNode nodetemp = ((Node)node).getAugmentation(SampleTestNode.class);
            if(nodetemp != null) {
                System.out.println("Nodetemp is .................");
                System.out.println(nodetemp.getClass().getSimpleName());
            }
            else {
                System.out.println("$$$$$$$$$$$$$$$Nodetemp is NULL$$$$$$$$$$$$$$$$$$$$");
            }
        }
        if(node instanceof SampleTestNode) {
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
            System.out.println("$$$$$$$$$$$SampleTestNode found.....$$$$$$$$$$$$$$$$$");
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
       }
    }

}
