package org.opendaylight.controller.protocol_plugin.openflow;

import java.util.List;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;
import org.opendaylight.controller.sal.reader.NodeTableStatistics;

/**
 * The Interface provides notification of statistics (hardware view) updates to
 * ReaderFilter listeners within the protocol plugin
 */
public interface IReadFilterInternalListener {

    /**
     * Notifies the hardware view of all the flow installed on the specified
     * network node was updated
     *
     * @param node
     *            the network node
     * @param flowStatsList
     */
    public void nodeFlowStatisticsUpdated(Node node, List<FlowOnNode> flowStatsList);

    /**
     * Notifies the hardware view of the specified network node connectors was
     * updated
     *
     * @param node
     *            the network node
     */
    public void nodeConnectorStatisticsUpdated(Node node, List<NodeConnectorStatistics> ncStatsList);

    /**
     * Notifies the hardware view of the specified network node tables was
     * updated
     *
     * @param node
     *            the network node
     */
    public void nodeTableStatisticsUpdated(Node node, List<NodeTableStatistics> tableStatsList);

    /**
     * Notifies the hardware view of all the flow installed on the specified
     * network node was updated
     *
     * @param node
     *            the network node
     */
    public void nodeDescriptionStatisticsUpdated(Node node, NodeDescription nodeDescription);
}
