package org.opendaylight.controller.md.frm.compatibility;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.Flows;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.FlowKey;

public interface FlowManagementReader {

    Flows readAllFlows();

    Flow readFlow(FlowKey key);

}
