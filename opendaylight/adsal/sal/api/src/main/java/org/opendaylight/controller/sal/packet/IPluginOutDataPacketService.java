
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   IPluginOutDataPacketService.java
 *
 * @brief  Interface SAL will need to register in order to get some
 * packets from the southbound protocol plugins
 *
 * Interface SAL will need to register in order to get some
 * packets from the southbound protocol plugins
 */

package org.opendaylight.controller.sal.packet;

/**
 * Interface used by SAL to intercept any Data Packet coming from the
 * southbound protocol plugins
 */
public interface IPluginOutDataPacketService {
    /**
     * Handler for receiving the packet. The SAL layer can signal back
     * to the southbound plugin if the packet has been consumed or can
     * go for further processing. Usually after SAL processing
     * probably there is no other processing to be done, but just in
     * case there is chain the return code can be used.
     * The protocol plugin is supposed to deliver a packet with the
     * IncomingNodeConnector set
     *
     * @param inPkt Packet received
     *
     * @return An indication if the packet should still be processed
     * or we should stop it.
     */
    PacketResult receiveDataPacket(RawPacket inPkt);
}
