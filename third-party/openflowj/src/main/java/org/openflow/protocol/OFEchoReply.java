package org.openflow.protocol;

import org.openflow.util.U16;

/**
 * Represents an ofp_echo_reply message
 * 
 * @author Rob Sherwood (rob.sherwood@stanford.edu)
 */

public class OFEchoReply extends OFEchoRequest {
    public static int MINIMUM_LENGTH = 8;

    public OFEchoReply() {
        super();
        this.type = OFType.ECHO_REPLY;
        this.length = U16.t(MINIMUM_LENGTH);
    }
}
