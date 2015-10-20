package org.openflow.protocol.action;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFActionDataLayerDestination extends OFActionDataLayer {
    public OFActionDataLayerDestination() {
        super();
        super.setType(OFActionType.SET_DL_DST);
        super.setLength((short) OFActionDataLayer.MINIMUM_LENGTH);
    }
}
