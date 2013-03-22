package org.openflow.protocol;

import org.openflow.util.U16;

/**
 * Represents an ofp_stats_reply message
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFStatisticsReply extends OFStatisticsMessageBase {
    public enum OFStatisticsReplyFlags {
        REPLY_MORE      (1 << 0);

        protected short type;

        OFStatisticsReplyFlags(int type) {
            this.type = (short) type;
        }

        public short getTypeValue() {
            return type;
        }
    }

    public OFStatisticsReply() {
        super();
        this.type = OFType.STATS_REPLY;
        this.length = U16.t(OFStatisticsMessageBase.MINIMUM_LENGTH);
    }
}
