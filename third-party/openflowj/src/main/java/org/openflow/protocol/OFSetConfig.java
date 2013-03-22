package org.openflow.protocol;

/**
 * Represents an OFPT_SET_CONFIG type message
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFSetConfig extends OFSwitchConfig {
    public OFSetConfig() {
        super();
        this.type = OFType.SET_CONFIG;
    }
}
