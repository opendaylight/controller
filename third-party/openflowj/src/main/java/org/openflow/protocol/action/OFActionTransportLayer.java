/**
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
package org.openflow.protocol.action;

import java.nio.ByteBuffer;

/**
 * Represents an ofp_action_tp_port
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
public abstract class OFActionTransportLayer extends OFAction {
    public static int MINIMUM_LENGTH = 8;

    protected short transportPort;

    /**
     * @return the transportPort
     */
    public short getTransportPort() {
        return transportPort;
    }

    /**
     * @param transportPort the transportPort to set
     */
    public void setTransportPort(short transportPort) {
        this.transportPort = transportPort;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        this.transportPort = data.getShort();
        data.getShort();
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.putShort(this.transportPort);
        data.putShort((short) 0);
    }

    @Override
    public int hashCode() {
        final int prime = 373;
        int result = super.hashCode();
        result = prime * result + transportPort;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof OFActionTransportLayer)) {
            return false;
        }
        OFActionTransportLayer other = (OFActionTransportLayer) obj;
        if (transportPort != other.transportPort) {
            return false;
        }
        return true;
    }
}