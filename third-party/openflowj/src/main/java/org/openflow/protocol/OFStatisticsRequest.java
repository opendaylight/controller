package org.openflow.protocol;

import org.openflow.util.U16;

/**
 * Represents an ofp_stats_request message
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFStatisticsRequest extends OFStatisticsMessageBase {
    public OFStatisticsRequest() {
        super();
        this.type = OFType.STATS_REQUEST;
        this.length = U16.t(OFStatisticsMessageBase.MINIMUM_LENGTH);
    }
}
