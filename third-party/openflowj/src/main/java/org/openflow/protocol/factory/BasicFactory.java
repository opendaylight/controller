package org.openflow.protocol.factory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionType;
import org.openflow.protocol.queue.OFQueueProperty;
import org.openflow.protocol.queue.OFQueuePropertyType;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.protocol.statistics.OFVendorStatistics;


/**
 * A basic OpenFlow factory that supports naive creation of both Messages and
 * Actions.
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 * @author Rob Sherwood (rob.sherwood@stanford.edu)
 *
 */
public class BasicFactory implements OFMessageFactory, OFActionFactory,
        OFQueuePropertyFactory, OFStatisticsFactory {
    @Override
    public OFMessage getMessage(OFType t) {
        return t.newInstance();
    }

    @Override
    public List<OFMessage> parseMessages(ByteBuffer data) {
        return parseMessages(data, 0);
    }

    @Override
    public List<OFMessage> parseMessages(ByteBuffer data, int limit) {
        List<OFMessage> results = new ArrayList<OFMessage>();
        OFMessage demux = new OFMessage();
        OFMessage ofm;

        while (limit == 0 || results.size() <= limit) {
            if (data.remaining() < OFMessage.MINIMUM_LENGTH)
                return results;

            data.mark();
            demux.readFrom(data);
            data.reset();

            if (demux.getLengthU() > data.remaining())
                return results;

            ofm = getMessage(demux.getType());
            if (ofm instanceof OFActionFactoryAware) {
                ((OFActionFactoryAware)ofm).setActionFactory(this);
            }
            if (ofm instanceof OFMessageFactoryAware) {
                ((OFMessageFactoryAware)ofm).setMessageFactory(this);
            }
            if (ofm instanceof OFQueuePropertyFactoryAware) {
                ((OFQueuePropertyFactoryAware)ofm).setQueuePropertyFactory(this);
            }
            if (ofm instanceof OFStatisticsFactoryAware) {
                ((OFStatisticsFactoryAware)ofm).setStatisticsFactory(this);
            }
            ofm.readFrom(data);
            if (OFMessage.class.equals(ofm.getClass())) {
                // advance the position for un-implemented messages
                data.position(data.position()+(ofm.getLengthU() -
                        OFMessage.MINIMUM_LENGTH));
            }
            results.add(ofm);
        }

        return results;
    }

    @Override
    public OFAction getAction(OFActionType t) {
        return t.newInstance();
    }

    @Override
    public List<OFAction> parseActions(ByteBuffer data, int length) {
        return parseActions(data, length, 0);
    }

    @Override
    public List<OFAction> parseActions(ByteBuffer data, int length, int limit) {
        List<OFAction> results = new ArrayList<OFAction>();
        OFAction demux = new OFAction();
        OFAction ofa;
        int end = data.position() + length;

        while (limit == 0 || results.size() <= limit) {
            if (data.remaining() < OFAction.MINIMUM_LENGTH ||
                    (data.position() + OFAction.MINIMUM_LENGTH) > end)
                return results;

            data.mark();
            demux.readFrom(data);
            data.reset();

            if (demux.getLengthU() > data.remaining() ||
                    (data.position() + demux.getLengthU()) > end)
                return results;

            ofa = getAction(demux.getType());
            ofa.readFrom(data);
            if (OFAction.class.equals(ofa.getClass())) {
                // advance the position for un-implemented messages
                data.position(data.position()+(ofa.getLengthU() -
                        OFAction.MINIMUM_LENGTH));
            }
            results.add(ofa);
        }

        return results;
    }

    @Override
    public OFActionFactory getActionFactory() {
        return this;
    }

    @Override
    public OFStatistics getStatistics(OFType t, OFStatisticsType st) {
        return st.newInstance(t);
    }

    @Override
    public List<OFStatistics> parseStatistics(OFType t, OFStatisticsType st,
            ByteBuffer data, int length) {
        return parseStatistics(t, st, data, length, 0);
    }

    /**
     * @param t
     *            OFMessage type: should be one of stats_request or stats_reply
     * @param st
     *            statistics type of this message, e.g., DESC, TABLE
     * @param data
     *            buffer to read from
     * @param length
     *            length of statistics
     * @param limit
     *            number of statistics to grab; 0 == all
     * 
     * @return list of statistics
     */

    @Override
    public List<OFStatistics> parseStatistics(OFType t, OFStatisticsType st,
            ByteBuffer data, int length, int limit) {
        List<OFStatistics> results = new ArrayList<OFStatistics>();
        OFStatistics statistics = getStatistics(t, st);

        int start = data.position();
        int count = 0;

        while (limit == 0 || results.size() <= limit) {
            // TODO Create a separate MUX/DEMUX path for vendor stats
            if (statistics instanceof OFVendorStatistics)
                ((OFVendorStatistics)statistics).setLength(length);

            /**
             * can't use data.remaining() here, b/c there could be other data
             * buffered past this message
             */
            if ((length - count) >= statistics.getLength()) {
                if (statistics instanceof OFActionFactoryAware)
                    ((OFActionFactoryAware)statistics).setActionFactory(this);
                statistics.readFrom(data);
                results.add(statistics);
                count += statistics.getLength();
                statistics = getStatistics(t, st);
            } else {
                if (count < length) {
                    /**
                     * Nasty case: partial/incomplete statistic found even
                     * though we have a full message. Found when NOX sent
                     * agg_stats request with wrong agg statistics length (52
                     * instead of 56)
                     * 
                     * just throw the rest away, or we will break framing
                     */
                    data.position(start + length);
                }
                return results;
            }
        }
        return results; // empty; no statistics at all
    }

    @Override
    public OFQueueProperty getQueueProperty(OFQueuePropertyType t) {
        return t.newInstance();
    }

    @Override
    public List<OFQueueProperty> parseQueueProperties(ByteBuffer data,
            int length) {
        return parseQueueProperties(data, length, 0);
    }

    @Override
    public List<OFQueueProperty> parseQueueProperties(ByteBuffer data,
            int length, int limit) {
        List<OFQueueProperty> results = new ArrayList<OFQueueProperty>();
        OFQueueProperty demux = new OFQueueProperty();
        OFQueueProperty ofqp;
        int end = data.position() + length;

        while (limit == 0 || results.size() <= limit) {
            if (data.remaining() < OFQueueProperty.MINIMUM_LENGTH ||
                    (data.position() + OFQueueProperty.MINIMUM_LENGTH) > end)
                return results;

            data.mark();
            demux.readFrom(data);
            data.reset();

            if (demux.getLengthU() > data.remaining() ||
                    (data.position() + demux.getLengthU()) > end)
                return results;

            ofqp = getQueueProperty(demux.getType());
            ofqp.readFrom(data);
            if (OFQueueProperty.class.equals(ofqp.getClass())) {
                // advance the position for un-implemented messages
                data.position(data.position()+(ofqp.getLengthU() -
                        OFQueueProperty.MINIMUM_LENGTH));
            }
            results.add(ofqp);
        }

        return results;
    }
}
