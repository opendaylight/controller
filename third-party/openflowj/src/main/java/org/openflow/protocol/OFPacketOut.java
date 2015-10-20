package org.openflow.protocol;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.factory.OFActionFactory;
import org.openflow.protocol.factory.OFActionFactoryAware;
import org.openflow.util.U16;

/**
 * Represents an ofp_packet_out message
 *
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 12, 2010
 */
public class OFPacketOut extends OFMessage implements OFActionFactoryAware {
    public static int MINIMUM_LENGTH = 16;
    public static int BUFFER_ID_NONE = 0xffffffff;

    protected OFActionFactory actionFactory;
    protected int bufferId;
    protected short inPort;
    protected short actionsLength;
    protected List<OFAction> actions;
    protected byte[] packetData;

    public OFPacketOut() {
        super();
        this.type = OFType.PACKET_OUT;
        this.length = U16.t(MINIMUM_LENGTH);
    }

    /**
     * Get buffer_id
     * @return
     */
    public int getBufferId() {
        return this.bufferId;
    }

    /**
     * Set buffer_id
     * @param bufferId
     */
    public OFPacketOut setBufferId(int bufferId) {
        this.bufferId = bufferId;
        return this;
    }

    /**
     * Returns the packet data
     * @return
     */
    public byte[] getPacketData() {
        return this.packetData;
    }

    /**
     * Sets the packet data
     * @param packetData
     */
    public OFPacketOut setPacketData(byte[] packetData) {
        this.packetData = packetData;
        return this;
    }

    /**
     * Get in_port
     * @return
     */
    public short getInPort() {
        return this.inPort;
    }

    /**
     * Set in_port
     * @param inPort
     */
    public OFPacketOut setInPort(short inPort) {
        this.inPort = inPort;
        return this;
    }

    /**
     * Set in_port. Convenience method using OFPort enum.
     * @param inPort
     */
    public OFPacketOut setInPort(OFPort inPort) {
        this.inPort = inPort.getValue();
        return this;
    }

    /**
     * Get actions_len
     * @return
     */
    public short getActionsLength() {
        return this.actionsLength;
    }

    /**
     * Get actions_len, unsigned
     * @return
     */
    public int getActionsLengthU() {
        return U16.f(this.actionsLength);
    }

    /**
     * Set actions_len
     * @param actionsLength
     */
    public OFPacketOut setActionsLength(short actionsLength) {
        this.actionsLength = actionsLength;
        return this;
    }

    /**
     * Returns the actions contained in this message
     * @return a list of ordered OFAction objects
     */
    public List<OFAction> getActions() {
        return this.actions;
    }

    /**
     * Sets the list of actions on this message
     * @param actions a list of ordered OFAction objects
     */
    public OFPacketOut setActions(List<OFAction> actions) {
        this.actions = actions;
        return this;
    }

    @Override
    public void setActionFactory(OFActionFactory actionFactory) {
        this.actionFactory = actionFactory;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        this.bufferId = data.getInt();
        this.inPort = data.getShort();
        this.actionsLength = data.getShort();
        if ( this.actionFactory == null)
            throw new RuntimeException("ActionFactory not set");
        this.actions = this.actionFactory.parseActions(data, getActionsLengthU());
        this.packetData = new byte[getLengthU() - MINIMUM_LENGTH - getActionsLengthU()];
        data.get(this.packetData);
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.putInt(bufferId);
        data.putShort(inPort);
        data.putShort(actionsLength);
        for (OFAction action : actions) {
            action.writeTo(data);
        }
        if (this.packetData != null)
            data.put(this.packetData);
    }

    @Override
    public int hashCode() {
        final int prime = 293;
        int result = super.hashCode();
        result = prime * result + ((actions == null) ? 0 : actions.hashCode());
        result = prime * result + actionsLength;
        result = prime * result + bufferId;
        result = prime * result + inPort;
        result = prime * result + Arrays.hashCode(packetData);
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
        if (!(obj instanceof OFPacketOut)) {
            return false;
        }
        OFPacketOut other = (OFPacketOut) obj;
        if (actions == null) {
            if (other.actions != null) {
                return false;
            }
        } else if (!actions.equals(other.actions)) {
            return false;
        }
        if (actionsLength != other.actionsLength) {
            return false;
        }
        if (bufferId != other.bufferId) {
            return false;
        }
        if (inPort != other.inPort) {
            return false;
        }
        if (!Arrays.equals(packetData, other.packetData)) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "OFPacketOut [actionFactory=" + actionFactory + ", actions="
                + actions + ", actionsLength=" + actionsLength + ", bufferId=0x"
                + Integer.toHexString(bufferId) + ", inPort=" + inPort + ", packetData="
                + Arrays.toString(packetData) + "]";
    }
}
