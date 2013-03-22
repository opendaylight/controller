package org.openflow.protocol.action;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFActionTransportLayerDestination extends OFActionTransportLayer {
    public OFActionTransportLayerDestination() {
        super();
        super.setType(OFActionType.SET_TP_DST);
        super.setLength((short) OFActionTransportLayer.MINIMUM_LENGTH);
    }
}
