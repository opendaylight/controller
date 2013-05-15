package org.opendaylight.controller.sal.rest.transform;

import org.opendaylight.controller.concepts.transform.AggregateTransformer;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.rest.flow.NodeFlowRTO;

public interface FlowOnNodeToRestTransformer extends AggregateTransformer<FlowOnNode, NodeFlowRTO>{

}
