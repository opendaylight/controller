package org.opendaylight.controller.protocol_plugin.openflow;

import java.util.List;

import org.openflow.protocol.statistics.OFStatistics;

/**
 * Interface which defines the notification functions which will get called when
 * the information contained in the OF statistics reply message received from a
 * network node is different from the cached one.
 */
public interface IOFStatisticsListener {
    /**
     * Notifies that a new list of description statistics objects for the given
     * switch is available
     *
     * @param switchId
     *            The datapath id of the openflow switch
     * @param description
     *            The new list of description statistics objects
     */
    public void descriptionStatisticsRefreshed(Long switchId, List<OFStatistics> description);

    /**
     * Notifies that a new list of flows statistics objects for the given switch
     * is available
     *
     * @param switchId
     *            The datapath id of the openflow switch
     * @param flows
     *            The new list of flow statistics objects
     */
    public void flowStatisticsRefreshed(Long switchId, List<OFStatistics> flows);

    /**
     * Notifies that a new list of port statistics objects for the given switch
     * is available
     *
     * @param switchId
     *            The datapath id of the openflow switch
     * @param flows
     *            The new list of port statistics objects
     */
    public void portStatisticsRefreshed(Long switchId, List<OFStatistics> ports);

    /**
     * Notifies that a new list of table statistics objects for the given switch
     * is available
     *
     * @param switchId
     *            The datapath id of the openflow switch
     * @param flows
     *            The new list of table statistics objects
     */
    public void tableStatisticsRefreshed(Long switchId, List<OFStatistics> tables);
}
