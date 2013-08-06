package org.opendaylight.controller.protocol_plugin.openflow.core.internal;

import java.util.ArrayList;
import java.util.List;

import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.OFDescriptionStatistics;

/**
 * Wrapper class to hold state for the OpenFlow switch connection
 * @author readams
 */
class OFChannelState {

    /**
     * State for handling the switch handshake
     */
    protected enum HandshakeState {
        /**
         * Beginning state
         */
        START,

        /**
         * Received HELLO from switch
         */
        HELLO,

        /**
         * We've received the features reply
         * Waiting for Config and Description reply
         */
        FEATURES_REPLY,

        /**
         * Switch is ready for processing messages
         */
        READY

    }

    protected volatile HandshakeState hsState = HandshakeState.START;
    protected boolean hasGetConfigReply = false;
    protected boolean hasDescription = false;
    protected boolean switchBindingDone = false;

    protected OFFeaturesReply featuresReply = null;
    protected OFDescriptionStatistics description = null;
    protected List<OFMessage> queuedOFMessages = new ArrayList<OFMessage>();
}