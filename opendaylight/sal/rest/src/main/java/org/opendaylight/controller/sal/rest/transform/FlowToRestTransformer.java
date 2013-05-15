package org.opendaylight.controller.sal.rest.transform;

import org.opendaylight.controller.concepts.transform.AggregateTransformer;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.rest.flow.FlowRTO;

public interface FlowToRestTransformer extends 
AggregateTransformer<Flow, FlowRTO>{

}
