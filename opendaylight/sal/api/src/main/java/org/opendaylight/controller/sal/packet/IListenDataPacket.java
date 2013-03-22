
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   IListenDataPacket.java
 *
 * @brief  Interface a component will need to implement and export as
 * implemented interface in order to get data packet
 *
 * Interface a component will need to implement and export as
 * implemented interface in order to get data packet
 */

package org.opendaylight.controller.sal.packet;

/**
 * Interface that all the components that want to receive a dataPacket
 * need to implement. The interface by itself doesn't specify any
 * filtering or sequencing mechanism, but the model supported by Data
 * Packet Service is such that the packets can be processed in two
 * ways:
 * - Serial: When a Data Packet Listener gets a packet after another,
 * this case is necessary when the subsequent handler needs some extra
 * information that can only be provided by another Data Packet
 * Service Handler. If the dependent service is missing, then the one
 * with dependencies will not be invoked. In a serial
 * processing, a plugin has the the power to prevent the packet to be
 * seen but other in the chain, if the return result is CONSUMED, else
 * the packet will go through all the elements in the chain.
 * - Parallel: When a Data Packet Listener doesn't express any
 * dependency then it will get a copy of the packet as anybody
 * else. Practical example, let say we have 2 handlers, both didn't
 * express any dependency then both will get a copy of any incoming
 * packet and they cannot step over each other feet.
 * The Processing model is choosen by the properties with which the
 * service is registered in the OSGi service registry.
 * The properties that will be looked at are:
 * salListenerName: Unique identifier of the SAL Data Packet
 * Listener
 * salListenerDependency: A String containing the
 * salListenerName that consitute a dependency for this Listener, for
 * now ONLY a SINGLE dependency is supported
 * salListenerFilter: A Match class to be used to match a DataPacket,
 * processing either parallel or serial will ONLY continue if the
 * incoming DataPacket match the filter. If no filter is provided, the
 * handler is called for EVERY packet i.e. match All is implied!
 */
public interface IListenDataPacket {
    /**
     * Handler for receiving the packet. The application can signal
     * back to SAL if the packet has been consumed or no. In case of
     * packet consumed SAL will prevent to keep passing to subsequent
     * listener in the serial chain, but for handlers without
     * dependency things will keep going .
     * The packet will received will have the IncomingNodeConnector
     * set to understand where the packet is coming from
     *
     * @param inPkt Packet received
     *
     * @return An indication if the packet should still be processed
     * or we should stop it.
     */
    PacketResult receiveDataPacket(RawPacket inPkt);
}
