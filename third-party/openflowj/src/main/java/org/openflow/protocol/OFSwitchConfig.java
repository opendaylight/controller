package org.openflow.protocol;

import java.nio.ByteBuffer;

/**
 * Base class representing ofp_switch_config based messages
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public abstract class OFSwitchConfig extends OFMessage {
    public static int MINIMUM_LENGTH = 12;

    public enum OFConfigFlags {
        OFPC_FRAG_NORMAL,
        OFPC_FRAG_DROP,
        OFPC_FRAG_REASM,
        OFPC_FRAG_MASK
    }

    protected short flags;
    protected short missSendLength;

    public OFSwitchConfig() {
        super();
        super.setLengthU(MINIMUM_LENGTH);
    }

    /**
     * @return the flags
     */
    public short getFlags() {
        return flags;
    }

    /**
     * @param flags the flags to set
     */
    public OFSwitchConfig setFlags(short flags) {
        this.flags = flags;
        return this;
    }

    /**
     * @return the missSendLength
     */
    public short getMissSendLength() {
        return missSendLength;
    }

    /**
     * @param missSendLength the missSendLength to set
     */
    public OFSwitchConfig setMissSendLength(short missSendLength) {
        this.missSendLength = missSendLength;
        return this;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        this.flags = data.getShort();
        this.missSendLength = data.getShort();
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.putShort(this.flags);
        data.putShort(this.missSendLength);
    }

    @Override
    public int hashCode() {
        final int prime = 331;
        int result = super.hashCode();
        result = prime * result + flags;
        result = prime * result + missSendLength;
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
        if (!(obj instanceof OFSwitchConfig)) {
            return false;
        }
        OFSwitchConfig other = (OFSwitchConfig) obj;
        if (flags != other.flags) {
            return false;
        }
        if (missSendLength != other.missSendLength) {
            return false;
        }
        return true;
    }
}
