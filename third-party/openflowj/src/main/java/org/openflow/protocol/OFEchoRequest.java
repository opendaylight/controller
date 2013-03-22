package org.openflow.protocol;

import java.nio.ByteBuffer;

import org.openflow.util.U16;

/**
 * Represents an ofp_echo_request message
 * 
 * @author Rob Sherwood (rob.sherwood@stanford.edu)
 */

public class OFEchoRequest extends OFMessage {
    public static int MINIMUM_LENGTH = 8;
    byte[] payload;

    public OFEchoRequest() {
        super();
        this.type = OFType.ECHO_REQUEST;
        this.length = U16.t(MINIMUM_LENGTH);
    }

    @Override
    public void readFrom(ByteBuffer bb) {
        super.readFrom(bb);
        int datalen = this.getLengthU() - MINIMUM_LENGTH;
        if (datalen > 0) {
            this.payload = new byte[datalen];
            bb.get(payload);
        }
    }

    /**
     * @return the payload
     */
    public byte[] getPayload() {
        return payload;
    }

    /**
     * @param payload
     *            the payload to set
     */
    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    @Override
    public void writeTo(ByteBuffer bb) {
        super.writeTo(bb);
        if (payload != null)
            bb.put(payload);
    }
}
