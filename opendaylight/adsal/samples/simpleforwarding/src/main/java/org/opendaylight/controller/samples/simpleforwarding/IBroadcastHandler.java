/*
 * Copyright (c) 2013 IBM and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.samples.simpleforwarding;

import org.opendaylight.controller.sal.packet.RawPacket;


/**
 * Provides support for flooding/broadcasting of packets.
 */
public interface IBroadcastHandler {

    /**
     * The mode to select which ports to broadcast a given packet. See the
     * individual modes for the expected behavior.
     */
    static enum BroadcastMode {
        /**
         * Turn off broadcast handling and ignore all received data packets.
         */
        DISABLED,

        /**
         * sends broadcast packets out ports where there are known hosts as
         * discovered by {@link ITopologyManager#getNodeConnectorWithHost}.
         */
        BROADCAST_TO_HOSTS,

        /**
         * sends broadcast packets out all non-internal links as discovered by
         * {@link ITopologyManager#isInternal}. Also ignores ports which have
         * {@link NodeConnector#getType} of "SW" indicating OFPP_LOCAL.
         */
        BROADCAST_TO_NONINTERNAL,

        /**
         * sends broadcast packets out the ports specified by an external
         * implementation of {@link IBroadcastPortSelector}.
         */
        EXTERNAL_QUERY
    };

    /**
     * Set the {@link BroadcastMode} for this {@link IBroadcastHandler}.
     * @param m
     */
    void setMode(BroadcastMode m);

    /**
     * Safely flood/broadcast a {@link RawPacket} received on a given
     * {@link NodeConnector}.
     *
     * @param pkt
     *            The packet to flood/broadcast
     * @return <tt>true</tt> if the broadcast is successful, <tt>false</tt>
     *         otherwise
     */
    boolean broadcastPacket(RawPacket pkt);
}
