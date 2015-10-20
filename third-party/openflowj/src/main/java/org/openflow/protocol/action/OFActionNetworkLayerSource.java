package org.openflow.protocol.action;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFActionNetworkLayerSource extends OFActionNetworkLayerAddress {
    public OFActionNetworkLayerSource() {
        super();
        super.setType(OFActionType.SET_NW_SRC);
        super.setLength((short) OFActionNetworkLayerAddress.MINIMUM_LENGTH);
    }
}
