package org.openflow.protocol;

import org.openflow.util.U16;


/**
 * Represents an ofp_hello message
 *
 * @author David Erickson (daviderickson@cs.stanford.edu) - Feb 8, 2010
 */
public class OFHello extends OFMessage {
    public static int MINIMUM_LENGTH = 8;

    /**
     * Construct a ofp_hello message
     */
    public OFHello() {
        super();
        this.type = OFType.HELLO;
        this.length = U16.t(MINIMUM_LENGTH);
    }
}
