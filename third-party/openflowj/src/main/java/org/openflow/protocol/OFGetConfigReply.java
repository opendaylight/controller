package org.openflow.protocol;

/**
 * Represents an OFPT_GET_CONFIG_REPLY type message
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFGetConfigReply extends OFSwitchConfig {
    public OFGetConfigReply() {
        super();
        this.type = OFType.GET_CONFIG_REPLY;
    }
}
