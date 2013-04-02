package org.opendaylight.controller.protocol_plugin.openflow;

import org.openflow.protocol.statistics.OFDescriptionStatistics;

/**
 * Interface which defines the api which gets called when the information
 * contained in the OF description statistics reply message from a network
 * is updated with new one.
 */
public interface IStatisticsListener {
	public void descriptionRefreshed(Long switchId,
					OFDescriptionStatistics description);
}
