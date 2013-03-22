package org.openflow.protocol;

import java.nio.ByteBuffer;

import org.openflow.util.U16;

/**
 * Represents an ofp_port_status message
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFPortStatus extends OFMessage {
    public static int MINIMUM_LENGTH = 64;

    public enum OFPortReason {
        OFPPR_ADD,
        OFPPR_DELETE,
        OFPPR_MODIFY
    }

    protected byte reason;
    protected OFPhysicalPort desc;

    /**
     * @return the reason
     */
    public byte getReason() {
        return reason;
    }

    /**
     * @param reason the reason to set
     */
    public void setReason(byte reason) {
        this.reason = reason;
    }

    /**
     * @return the desc
     */
    public OFPhysicalPort getDesc() {
        return desc;
    }

    /**
     * @param desc the desc to set
     */
    public void setDesc(OFPhysicalPort desc) {
        this.desc = desc;
    }

    public OFPortStatus() {
        super();
        this.type = OFType.PORT_STATUS;
        this.length = U16.t(MINIMUM_LENGTH);
    }

    @Override
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        this.reason = data.get();
        data.position(data.position() + 7); // skip 7 bytes of padding
        if (this.desc == null)
            this.desc = new OFPhysicalPort();
        this.desc.readFrom(data);
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.put(this.reason);
        for (int i = 0; i < 7; ++i)
            data.put((byte) 0);
        this.desc.writeTo(data);
    }

    @Override
    public int hashCode() {
        final int prime = 313;
        int result = super.hashCode();
        result = prime * result + ((desc == null) ? 0 : desc.hashCode());
        result = prime * result + reason;
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
        if (!(obj instanceof OFPortStatus)) {
            return false;
        }
        OFPortStatus other = (OFPortStatus) obj;
        if (desc == null) {
            if (other.desc != null) {
                return false;
            }
        } else if (!desc.equals(other.desc)) {
            return false;
        }
        if (reason != other.reason) {
            return false;
        }
        return true;
    }
}
