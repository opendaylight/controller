/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.protocol_plugin.openflow.mapping.api.OFMappingContext;
import org.opendaylight.controller.protocol_plugin.openflow.vendorextension.v6extension.V6StatsReply;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts an openflow list of flow statistics in a SAL list of FlowOnNode
 * objects
 * 
 * 
 * 
 */
public class FlowStatisticsConverter {
    private static final Logger log = LoggerFactory
            .getLogger(FlowStatisticsConverter.class);
    private List<OFStatistics> ofStatsList;
    private List<FlowOnNode> flowOnNodeList;
    private OFMappingContext mapping;

    public FlowStatisticsConverter(OFMappingContext mapping,List<OFStatistics> statsList) {
        this.mapping = mapping;
        if (statsList == null) {// || statsList.isEmpty()) {
            this.ofStatsList = new ArrayList<OFStatistics>(1); // dummy list
        } else {
            this.ofStatsList = statsList; // new
                                          // ArrayList<OFStatistics>(statsList);
        }
        this.flowOnNodeList = null;
    }

    public List<FlowOnNode> getFlowOnNodeList(Node node) {
        if (ofStatsList != null && flowOnNodeList == null) {
            flowOnNodeList = new ArrayList<FlowOnNode>();
            FlowConverter flowConverter = null;
            OFFlowStatisticsReply ofFlowStat;
            V6StatsReply v6StatsReply;
            for (OFStatistics ofStat : ofStatsList) {
                FlowOnNode flowOnNode = null;
                if (ofStat instanceof OFFlowStatisticsReply) {
                    ofFlowStat = (OFFlowStatisticsReply) ofStat;
                    flowConverter = new FlowConverter(mapping,ofFlowStat.getMatch(),
                            ofFlowStat.getActions());
                    Flow flow = flowConverter.getFlow(node);
                    flow.setPriority(ofFlowStat.getPriority());
                    flow.setIdleTimeout(ofFlowStat.getIdleTimeout());
                    flow.setHardTimeout(ofFlowStat.getHardTimeout());
                    flowOnNode = new FlowOnNode(flow);
                    flowOnNode.setByteCount(ofFlowStat.getByteCount());
                    flowOnNode.setPacketCount(ofFlowStat.getPacketCount());
                    flowOnNode.setDurationSeconds(ofFlowStat
                            .getDurationSeconds());
                    flowOnNode.setDurationNanoseconds(ofFlowStat
                            .getDurationNanoseconds());
                } else if (ofStat instanceof V6StatsReply) {
                    v6StatsReply = (V6StatsReply) ofStat;
                    flowConverter = new FlowConverter(mapping,v6StatsReply.getMatch(),
                            v6StatsReply.getActions());
                    Flow flow = flowConverter.getFlow(node);
                    flow.setPriority(v6StatsReply.getPriority());
                    flow.setIdleTimeout(v6StatsReply.getIdleTimeout());
                    flow.setHardTimeout(v6StatsReply.getHardTimeout());
                    flowOnNode = new FlowOnNode(flow);
                    flowOnNode.setByteCount(v6StatsReply.getByteCount());
                    flowOnNode.setPacketCount(v6StatsReply.getPacketCount());
                    flowOnNode.setDurationSeconds(v6StatsReply
                            .getDurationSeconds());
                    flowOnNode.setDurationNanoseconds(v6StatsReply
                            .getDurationNanoseconds());
                } else {
                    continue;
                }
                flowOnNodeList.add(flowOnNode);
            }
        }
        log.trace("OFStatistics: {} FlowOnNode: {}", ofStatsList,
                flowOnNodeList);
        return flowOnNodeList;
    }
}
