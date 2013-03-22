
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

import org.openflow.protocol.statistics.OFPortStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.utils.NodeCreator;

/**
 * Converts an openflow list of port statistics in a SAL list of
 * NodeConnectorStatistics objects
 *
 *
 *
 */
public class PortStatisticsConverter {
    private long switchId;
    private List<OFStatistics> ofStatsList;
    private List<NodeConnectorStatistics> ncStatsList;

    public PortStatisticsConverter(long switchId, List<OFStatistics> statsList) {
        this.switchId = switchId;
        if (statsList == null || statsList.isEmpty()) {
            this.ofStatsList = new ArrayList<OFStatistics>(1); // dummy list
        } else {
            this.ofStatsList = new ArrayList<OFStatistics>(statsList);
        }
        this.ncStatsList = null;
    }

    public List<NodeConnectorStatistics> getNodeConnectorStatsList() {
        if (this.ofStatsList != null && this.ncStatsList == null) {
            this.ncStatsList = new ArrayList<NodeConnectorStatistics>();
            OFPortStatisticsReply ofPortStat;
            Node node = NodeCreator.createOFNode(switchId);
            for (OFStatistics ofStat : this.ofStatsList) {
                ofPortStat = (OFPortStatisticsReply) ofStat;
                NodeConnectorStatistics NCStat = new NodeConnectorStatistics();
                NCStat.setNodeConnector(PortConverter.toNodeConnector(
                        ofPortStat.getPortNumber(), node));
                NCStat.setReceivePacketCount(ofPortStat.getreceivePackets());
                NCStat.setTransmitPacketCount(ofPortStat.getTransmitPackets());
                NCStat.setReceiveByteCount(ofPortStat.getReceiveBytes());
                NCStat.setTransmitByteCount(ofPortStat.getTransmitBytes());
                NCStat.setReceiveDropCount(ofPortStat.getReceiveDropped());
                NCStat.setTransmitDropCount(ofPortStat.getTransmitDropped());
                NCStat.setReceiveErrorCount(ofPortStat.getreceiveErrors());
                NCStat.setTransmitErrorCount(ofPortStat.getTransmitErrors());
                NCStat.setReceiveFrameErrorCount(ofPortStat
                        .getReceiveFrameErrors());
                NCStat.setReceiveOverRunErrorCount(ofPortStat
                        .getReceiveOverrunErrors());
                NCStat
                        .setReceiveCRCErrorCount(ofPortStat
                                .getReceiveCRCErrors());
                NCStat.setCollisionCount(ofPortStat.getCollisions());
                this.ncStatsList.add(NCStat);
            }
        }
        return this.ncStatsList;
    }

}
