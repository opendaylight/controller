/*
 * Copyright (c) 2013 Big Switch Networks, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.protocol_plugin.openflow.internal;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.reader.NodeTableStatistics;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFTableStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts an openflow list of table statistics in a SAL list of
 * NodeTableStatistics objects
 */
public class TableStatisticsConverter {
    private static final Logger log = LoggerFactory
            .getLogger(TableStatisticsConverter.class);

    private final long switchId;
    private List<OFStatistics> ofStatsList;
    private List<NodeTableStatistics> ntStatsList;

    public TableStatisticsConverter(long switchId, List<OFStatistics> statsList) {
        this.switchId = switchId;
        if (statsList == null || statsList.isEmpty()) {
            this.ofStatsList = new ArrayList<OFStatistics>(1); // dummy list
        } else {
            this.ofStatsList = new ArrayList<OFStatistics>(statsList);
        }
        this.ntStatsList = null;
    }

    public List<NodeTableStatistics> getNodeTableStatsList() {
        if (this.ofStatsList != null && this.ntStatsList == null) {
            this.ntStatsList = new ArrayList<NodeTableStatistics>();
            OFTableStatistics ofTableStat;
            Node node = NodeCreator.createOFNode(switchId);
            for (OFStatistics ofStat : this.ofStatsList) {
                ofTableStat = (OFTableStatistics) ofStat;
                NodeTableStatistics ntStat = new NodeTableStatistics();
                ntStat.setNodeTable(TableConverter.toNodeTable(
                        ofTableStat.getTableId(), node));
                ntStat.setActiveCount(ofTableStat.getActiveCount());
                ntStat.setLookupCount(ofTableStat.getLookupCount());
                ntStat.setMatchedCount(ofTableStat.getMatchedCount());
                this.ntStatsList.add(ntStat);
            }
        }
        log.trace("OFStatistics: {} NodeTableStatistics: {}", ofStatsList,
                ntStatsList);
        return this.ntStatsList;
    }
}
