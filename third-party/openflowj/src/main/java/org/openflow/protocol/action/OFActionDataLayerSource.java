package org.openflow.protocol.action;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFActionDataLayerSource extends OFActionDataLayer {
    public OFActionDataLayerSource() {
        super();
        super.setType(OFActionType.SET_DL_SRC);
        super.setLength((short) OFActionDataLayer.MINIMUM_LENGTH);
    }
}
