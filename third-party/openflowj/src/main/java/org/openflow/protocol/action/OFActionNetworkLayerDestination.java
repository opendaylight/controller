package org.openflow.protocol.action;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFActionNetworkLayerDestination extends OFActionNetworkLayerAddress {
    public OFActionNetworkLayerDestination() {
        super();
        super.setType(OFActionType.SET_NW_DST);
        super.setLength((short) OFActionNetworkLayerAddress.MINIMUM_LENGTH);
    }
}
