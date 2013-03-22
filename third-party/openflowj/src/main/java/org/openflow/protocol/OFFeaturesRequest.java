package org.openflow.protocol;

import org.openflow.util.U16;


/**
 * Represents a features request message
 * @author David Erickson (daviderickson@cs.stanford.edu)
 *
 */
public class OFFeaturesRequest extends OFMessage {
    public static int MINIMUM_LENGTH = 8;

    public OFFeaturesRequest() {
        super();
        this.type = OFType.FEATURES_REQUEST;
        this.length = U16.t(MINIMUM_LENGTH);
    }
}
