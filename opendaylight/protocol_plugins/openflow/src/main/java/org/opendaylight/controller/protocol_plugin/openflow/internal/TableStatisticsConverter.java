/**
 * 
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
 * @author adityavaja
 * Converts an openflow list of table statistics in a SAL list of
 * NodeTableStatistics objects
 */
public class TableStatisticsConverter {
    private static final Logger log = LoggerFactory
            .getLogger(TableStatisticsConverter.class);
    
    private long switchId;
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
                NodeTableStatistics NTStat = new NodeTableStatistics();
                NTStat.setNodeTable(TableConverter.toNodeTable(
                		ofTableStat.getTableId(), node));
                NTStat.setActiveCount(ofTableStat.getActiveCount());
                NTStat.setLookupCount(ofTableStat.getLookupCount());
                NTStat.setMatchedCount(ofTableStat.getMatchedCount());
                this.ntStatsList.add(NTStat);
            }
        }
        log.trace("OFStatistics: {} NodeTableStatistics: {}", ofStatsList,
                ntStatsList);
        return this.ntStatsList;
    }
}
