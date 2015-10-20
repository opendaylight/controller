package org.openflow.protocol;

import java.io.Serializable;
import java.nio.ByteBuffer;

import org.openflow.util.U16;
import org.openflow.util.U32;
import org.openflow.util.U8;

/**
 * The base class for all OpenFlow protocol messages. This class contains the
 * equivalent of the ofp_header which is present in all OpenFlow messages.
 *
 * @author David Erickson (daviderickson@cs.stanford.edu) - Feb 3, 2010
 * @author Rob Sherwood (rob.sherwood@stanford.edu) - Feb 3, 2010
 */
public class OFMessage implements Serializable{
    public static byte OFP_VERSION = 0x01;
    public static int MINIMUM_LENGTH = 8;

    protected byte version;
    protected OFType type;
    protected short length;
    protected int xid;

    public OFMessage() {
        this.version = OFP_VERSION;
    }

    /**
     * Get the length of this message
     *
     * @return
     */
    public short getLength() {
        return length;
    }

    /**
     * Get the length of this message, unsigned
     *
     * @return
     */
    public int getLengthU() {
        return U16.f(length);
    }

    /**
     * Set the length of this message
     *
     * @param length
     */
    public OFMessage setLength(short length) {
        this.length = length;
        return this;
    }

    /**
     * Set the length of this message, unsigned
     *
     * @param length
     */
    public OFMessage setLengthU(int length) {
        this.length = U16.t(length);
        return this;
    }

    /**
     * Get the type of this message
     *
     * @return
     */
    public OFType getType() {
        return type;
    }

    /**
     * Set the type of this message
     *
     * @param type
     */
    public void setType(OFType type) {
        this.type = type;
    }

    /**
     * Get the OpenFlow version of this message
     *
     * @return
     */
    public byte getVersion() {
        return version;
    }

    /**
     * Set the OpenFlow version of this message
     *
     * @param version
     */
    public void setVersion(byte version) {
        this.version = version;
    }

    /**
     * Get the transaction id of this message
     *
     * @return
     */
    public int getXid() {
        return xid;
    }

    /**
     * Set the transaction id of this message
     *
     * @param xid
     */
    public void setXid(int xid) {
        this.xid = xid;
    }

    /**
     * Read this message off the wire from the specified ByteBuffer
     * @param data
     */
    public void readFrom(ByteBuffer data) {
        this.version = data.get();
        this.type = OFType.valueOf(data.get());
        this.length = data.getShort();
        this.xid = data.getInt();
    }

    /**
     * Write this message's binary format to the specified ByteBuffer
     * @param data
     */
    public void writeTo(ByteBuffer data) {
        data.put(version);
        data.put(type.getTypeValue());
        data.putShort(length);
        data.putInt(xid);
    }

    /**
     * Returns a summary of the message
     * @return "ofmsg=v=$version;t=$type:l=$len:xid=$xid"
     */
    public String toString() {
        return "ofmsg" +
            ":v=" + U8.f(this.getVersion()) +
            ";t=" + this.getType() +
            ";l=" + this.getLengthU() +
            ";x=" + U32.f(this.getXid());
    }

    @Override
    public int hashCode() {
        final int prime = 97;
        int result = 1;
        result = prime * result + length;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + version;
        result = prime * result + xid;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OFMessage)) {
            return false;
        }
        OFMessage other = (OFMessage) obj;
        if (length != other.length) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        if (version != other.version) {
            return false;
        }
        if (xid != other.xid) {
            return false;
        }
        return true;
    }
}
