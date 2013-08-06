package org.opendaylight.controller.protocol_plugin.openflow;

import java.util.List;

import org.openflow.protocol.statistics.OFStatistics;

/**
 * @file   IStatisticsShimListener.java
 *
 * @brief  Statistics notifications provided by SAL toward the application
 *
 * For example an application that wants to keep up to date with the
 * updates coming from SAL it will register in the OSGi service
 * registry this interface (on a per-container base) and SAL will call it
 * providing the update
 */

/**
 * Statistics notifications provided by SAL toward the application
 *
 */
public interface IStatisticsServiceShimListener {

    /**
     * Called to update statistics
     *
     * @param
     * @param
     * @param
     */
    public void flowStatisticsUpdate(Long switchId,
                        List<OFStatistics> value, String containerName);

    public void descStatisticsUpdate(Long switchId,
                        List<OFStatistics> value, String containerName);

    public void portStatisticsUpdate(Long switchId,
                        List<OFStatistics> value, String containerName);

    public void flowTableStatisticsUpdate(Long switchId,
                        List<OFStatistics> value, String containerName);

}
