package org.opendaylight.controller.sal.rest.transform.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.rest.flow.FlowRTO;
import org.opendaylight.controller.sal.rest.flow.NodeFlowRTO;
import org.opendaylight.controller.sal.rest.transform.FlowOnNodeToRestTransformer;
import org.opendaylight.controller.sal.rest.transform.FlowToRestTransformer;

public class FlowOnNodeToRestTransformerImpl implements
        FlowOnNodeToRestTransformer {

    private FlowToRestTransformer flowTransformer;

    public FlowToRestTransformer getFlowTransformer() {
        return flowTransformer;
    }

    public void setFlowTransformer(FlowToRestTransformer flowTransformer) {
        this.flowTransformer = flowTransformer;
    }

    public void unsetFlowTransformer() {
        this.flowTransformer = null;
    }
    
    @Override
    public Collection<NodeFlowRTO> transformAll(
            Collection<? extends FlowOnNode> inputs) {
        List<NodeFlowRTO> ret = new ArrayList<NodeFlowRTO>();
        for (FlowOnNode flowOnNode : inputs) {
            ret.add(transform(flowOnNode));
        }

        return ret;
    }

    @Override
    public NodeFlowRTO transform(FlowOnNode input) {
        FlowRTO flow = flowTransformer.transform(input.getFlow());

        NodeFlowRTO prod = new NodeFlowRTO(flow);
        prod.setByteCount(input.getByteCount());
        prod.setDurationNanoseconds(input.getDurationNanoseconds());
        prod.setDurationSeconds(input.getDurationSeconds());
        prod.setPacketCount(input.getPacketCount());
        prod.setTableId(input.getTableId());

        return prod;
    }

}
