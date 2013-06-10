package org.opendaylight.controller.samples.simpleforwarding;

import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.topologymanager.ITopologyManager;

/**
 * Provides support for flooding/broadcasting of packets.
 *
 * @author Colin Dixon <ckd@us.ibm.com>
 */
public interface IBroadcastHandler {

    /**
     * The mode to select which ports to broadcast a given packet. See the
     * individual modes for the expected behavior.
     *
     * @author Colin Dixon <ckd@us.ibm.com>
     */
    public static enum Mode {
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
     * Set the {@link Mode} for this {@link IBroadcastHandler}.
     * @param m
     */
    void setMode(Mode m);

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
