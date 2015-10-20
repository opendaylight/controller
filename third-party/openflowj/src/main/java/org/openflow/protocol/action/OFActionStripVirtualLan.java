/**
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
package org.openflow.protocol.action;

import java.nio.ByteBuffer;


/**
 * Represents an ofp_action_strip_vlan
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
public class OFActionStripVirtualLan extends OFAction {
    public static int MINIMUM_LENGTH = 8;

    public OFActionStripVirtualLan() {
        super();
        super.setType(OFActionType.STRIP_VLAN);
        super.setLength((short) MINIMUM_LENGTH);
    }

    @Override
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        // PAD
        data.getInt();
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        // PAD
        data.putInt(0);
    }
}