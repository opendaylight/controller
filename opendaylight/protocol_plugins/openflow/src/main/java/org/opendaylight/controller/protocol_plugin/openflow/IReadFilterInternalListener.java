package org.opendaylight.controller.protocol_plugin.openflow;

import java.util.List;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;
import org.opendaylight.controller.sal.reader.NodeTableStatistics;

/**
 * The Interface provides statistics updates to ReaderFilter listeners within
 * the protocol plugin
 */
public interface IReadFilterInternalListener {

    /**
     * Notifies the hardware view of all the flow installed on the specified network node
     * @param node
     * @return
     */
    public void nodeFlowStatisticsUpdated(Node node, List<FlowOnNode> flowStatsList);

    /**
     * Notifies the hardware view of the specified network node connector
     * @param node
     * @return
     */
    public void nodeConnectorStatisticsUpdated(Node node, List<NodeConnectorStatistics> ncStatsList);

    /**
     * Notifies all the table statistics for a node
     * @param node
     * @return
     */
    public void nodeTableStatisticsUpdated(Node node, List<NodeTableStatistics> tableStatsList);

    /**
     * Notifies the hardware view of all the flow installed on the specified network node
     * @param node
     * @return
     */
    public void nodeDescriptionStatisticsUpdated(Node node, NodeDescription nodeDescription);


}
