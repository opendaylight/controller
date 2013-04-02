
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow;

import java.util.List;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;

/**
 * Interface to expose the openflow statistics collected on the switches
 */
public interface IOFStatisticsManager {
    /**
     * Return all the statistics for all the flows present on the specified switch
     *
	 * @param switchId the openflow datapath id
	 * @return	the list of openflow statistics
	 */
    List<OFStatistics> getOFFlowStatistics(Long switchId);

    /**
     * Return all the statistics for all the flows present on the specified switch
     *
     * @param switchId the openflow datapath id
     * @param ofMatch the openflow match to query. If null, the query is intended for all the flows
     * @return the list of openflow statistics
     */
    List<OFStatistics> getOFFlowStatistics(Long switchId, OFMatch ofMatch);

    /**
     * Return the description statistics for the specified switch.
     *
	 * @param switchId the openflow datapath id
     * @return the list of openflow statistics
     */
    List<OFStatistics> getOFDescStatistics(Long switchId);

    /**
     * Returns the statistics for all the ports on the specified switch
     *
	 * @param switchId the openflow datapath id
     * @return the list of openflow statistics
     */
    List<OFStatistics> getOFPortStatistics(Long switchId);

    /**
     * Returns the statistics for the specified switch port
     *
	 * @param switchId the openflow datapath id
     * @param portId the openflow switch port id
     * @return the list of openflow statistics
     */
    List<OFStatistics> getOFPortStatistics(Long switchId, short portId);

    /**
     * Returns the number of flows installed on the switch
     *
	 * @param switchId the openflow datapath id
     * @return the number of flows installed on the switch
     */
    int getFlowsNumber(long switchId);

    /**
     * Send a statistics request message to the specified switch and returns
     * the switch response. It blocks the caller until the response has arrived
     * from the switch or the request has timed out
     *
     * @param switchId the openflow datapath id of the target switch
     * @param statType the openflow statistics type
     * @param target the target object. For flow statistics it is the OFMatch.
     * 				 For port statistics, it is the port id. If null the query
     * 				 will be performed for all the targets for the specified
     * 				 statistics type. 
     * 				 
     * @param timeout the timeout in milliseconds the system will wait for a response
     * 		  from the switch, before declaring failure 
     * @return the list of openflow statistics
     */
    List<OFStatistics> queryStatistics(Long switchId,
            OFStatisticsType statType, Object target);

    /**
     * Returns the averaged transmit rate for the passed switch port
     *
     * @param switchId the openflow datapath id of the target switch
     * @param portId the openflow switch port id
     * @return the median transmit rate in bits per second
     */
    long getTransmitRate(Long switchId, Short port);

}
