package org.opendaylight.controller.forwardingrulesmanager.impl;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsInputBuilder;

public class FlowStatisticsUtils {

    public static GetFlowStatisticsInputBuilder flowStatisticsRequest(Flow flow) {
        GetFlowStatisticsInputBuilder builder = new GetFlowStatisticsInputBuilder();
        builder.setAction(flow.getAction());
        builder.setCookie(flow.getCookie());
        builder.setHardTimeout(flow.getHardTimeout()); // Are timeouts necessary?
        builder.setIdleTimeout(flow.getIdleTimeout()); // Are timeouts necessary?
        builder.setNode(flow.getNode());
        builder.setPriority(flow.getPriority());
        builder.setMatch(flow.getMatch());
        return builder;
    }
}
