
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   IPluginInDataPacketService.java
 *
 * @brief  Data Packet Services exported by SouthBound plugins for SAL usage
 *
 * Data Packet Services exported by SouthBound plugins for SAL usage
 */
package org.opendaylight.controller.sal.packet;

/**
 * Data Packet Services exported by SouthBound plugins for SAL usage.
 * The service will be used by SAL such that for every Protocol Plugin
 * there is only one expected, for this reason the service need to be
 * registered in the OSGi service registry along with the property:
 * - "protocoloPluginType"
 * the value of the property will org.opendaylight.controller.sal.core.Node.NodeIDType
 */
public interface IPluginInDataPacketService {
    /**
     * Transmit a data Packet. Packet will go out ONLY if the packet
     * has property OutgoingNodeConnector set.
     *
     * @param outPkt Packet to be transmitted out
     */
    void transmitDataPacket(RawPacket outPkt);
}
