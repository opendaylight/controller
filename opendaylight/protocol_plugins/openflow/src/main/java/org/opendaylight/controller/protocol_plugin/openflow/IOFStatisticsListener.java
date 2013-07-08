package org.opendaylight.controller.protocol_plugin.openflow;

import java.util.List;

import org.openflow.protocol.statistics.OFStatistics;

/**
 * Interface defines the api which gets called when the information
 * contained in the OF statistics reply message from a network is updated with
 * new one.
 */
public interface IOFStatisticsListener {
    public void descriptionStatisticsRefreshed(Long switchId, List<OFStatistics> description);

    public void flowStatisticsRefreshed(Long switchId, List<OFStatistics> flows);

    public void portStatisticsRefreshed(Long switchId, List<OFStatistics> ports);

    public void tableStatisticsRefreshed(Long switchId, List<OFStatistics> tables);
}
