package org.openflow.protocol.action;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFActionTransportLayerSource extends OFActionTransportLayer {
    public OFActionTransportLayerSource() {
        super();
        super.setType(OFActionType.SET_TP_SRC);
        super.setLength((short) OFActionTransportLayer.MINIMUM_LENGTH);
    }
}
