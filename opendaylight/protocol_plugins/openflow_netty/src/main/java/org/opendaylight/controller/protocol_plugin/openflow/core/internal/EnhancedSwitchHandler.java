package org.opendaylight.controller.protocol_plugin.openflow.core.internal;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedSelectorException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.opendaylight.controller.protocol_plugin.openflow.core.IEnhancedSwitch;
import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitch;
import org.opendaylight.controller.protocol_plugin.openflow.core.internal.SwitchEvent.SwitchEventType;
import org.openflow.protocol.OFBarrierReply;
import org.openflow.protocol.OFBarrierRequest;
import org.openflow.protocol.OFEchoReply;
import org.openflow.protocol.OFError;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFGetConfigReply;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFPortStatus;
import org.openflow.protocol.OFSetConfig;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.OFType;
import org.openflow.protocol.OFPhysicalPort.OFPortConfig;
import org.openflow.protocol.OFPhysicalPort.OFPortFeatures;
import org.openflow.protocol.OFPhysicalPort.OFPortState;
import org.openflow.protocol.OFPortStatus.OFPortReason;
import org.openflow.protocol.factory.BasicFactory;
import org.openflow.protocol.factory.MessageParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnhancedSwitchHandler implements IEnhancedSwitch {


    private static final Logger logger = LoggerFactory
            .getLogger(EnhancedSwitchHandler.class);
    private static final int switchLivenessTimeout = getSwitchLivenessTimeout();
    private int MESSAGE_RESPONSE_TIMER = 2000;

    private EnhancedController controller = null;
    private Integer switchChannelID = null;
    private Channel channel;
    private long lastMsgReceivedTimeStamp = 0;
    private SwitchState state = null;
    private BasicFactory factory = null;
    private HashedWheelTimer timer = null;
    private SwitchLivelinessTimerTask switchLivelinessTask = null;
    private Timeout switchLivelinessTaskHandle = null;
    private long sid;
    private AtomicInteger xid;
    private int buffers;
    private int capabilities;
    private byte tables;
    private int actions;
    private Map<Short, OFPhysicalPort> physicalPorts;
    private Map<Short, Integer> portBandwidth;
    private Date connectedDate;
    private ExecutorService executor = null;
    private ConcurrentHashMap<Integer, Callable<Object>> messageWaitingDone;
    private Integer responseTimerValue;
    private TrafficStatisticsHandler trafficStatsHandler = null;
    private static final boolean START_LIVELINESS_TIMER = false;

    private static final int BATCH_COUNT_FOR_FLUSHING = 3;
    private int flushBatchTrack = 0;

    /*
    private List<OFMessage> msgBuffer = new ArrayList<OFMessage>();
    private int bufferTrack = 0;
    private static final int BATCH_BUFFER_THRESHOLD = 100;
    */


    // PLEASE .. IF THERE IS SOMETHING CALLED GOD, HELP ME GET THE THROUGHPUT WITH THIS !!
    private List<OFMessage> flushableMsgBuffer = new ArrayList<OFMessage>();


    public enum SwitchState {
        NON_OPERATIONAL(0),
        WAIT_FEATURES_REPLY(1),
        WAIT_CONFIG_REPLY(2),
        OPERATIONAL(3);

        private int value;

        private SwitchState(int value) {
            this.value = value;
        }

        @SuppressWarnings("unused")
        public int value() {
            return this.value;
        }
    }


    public EnhancedSwitchHandler(EnhancedController controller,
            Integer switchConnectionChannelID,
            Channel channel,
            HashedWheelTimer timer,
            ExecutorService executor,
            TrafficStatisticsHandler tHandler){

        this.controller = controller;
        this.physicalPorts = new HashMap<Short, OFPhysicalPort>();
        this.portBandwidth = new HashMap<Short, Integer>();
        this.switchChannelID = switchConnectionChannelID;
        this.timer = timer;
        this.sid = (long) 0;
        this.tables = (byte) 0;
        this.actions = (int) 0;
        this.capabilities = (int) 0;
        this.buffers = (int) 0;
        this.connectedDate = new Date();
        this.state = SwitchState.NON_OPERATIONAL;
        this.executor = executor;
        this.messageWaitingDone = new ConcurrentHashMap<Integer, Callable<Object>>();
        this.responseTimerValue = MESSAGE_RESPONSE_TIMER;
        this.channel = channel;
        this.xid = new AtomicInteger(this.channel.hashCode());
        this.trafficStatsHandler = tHandler;

    }

    Integer getSwitchChannelID() {
        return this.switchChannelID;
    }

    public void startHandler(){
        this.factory = new BasicFactory();
        start();

    }


    public void shutDownHandler(){
        stop();

    }


    public void handleChannelIdle(){
        // TODO: this is already handled by OFChannelHandler
        // so DON'T care


    }


    public void start() {
        sendFirstHello();
    }

    public void stop() {
        cancelSwitchTimer();
        SwitchEvent ev = new SwitchEvent(SwitchEventType.SWITCH_DELETE, this, null);
        controller.switchDeleted(ev, switchChannelID);
    }

    private void cancelSwitchTimer() {
        if (switchLivelinessTaskHandle != null){
            this.switchLivelinessTaskHandle.cancel();
        }
    }


    public void handleCaughtException(){



    }




    @Override
    public int getNextXid() {
        return this.xid.incrementAndGet();
    }

    @Override
    public Long getId() {
        return this.sid;
    }

    @Override
    public Byte getTables() {
        return this.tables;
    }

    @Override
    public Integer getActions() {
        return this.actions;
    }

    @Override
    public Integer getCapabilities() {
        return this.capabilities;
    }

    @Override
    public Integer getBuffers() {
        return this.buffers;
    }

    @Override
    public Date getConnectedDate() {
        return this.connectedDate;
    }

    @Override
    public Integer asyncSend(OFMessage msg) {
        return asyncSend(msg, getNextXid());
    }


    @Override
    public Integer asyncSend(OFMessage msg, int xid) {
        // TODO:
        // BATCHING IMPLEMENTATION. Please think hard before enablng this !!
        // Some messages could be latency-sensitive and some could be batched
        // for better throughput. So, below decision may not bring better
        // throughput for latency-sensitive cases like FLOW-MODs or
        // PACKET-OUTs

        /*
        if (bufferTrack == BUFFER_THRESHOLD){
            this.channel.write(msgBuffer);
            msgBuffer.clear();
            bufferTrack = 0;

        }
        msg.setXid(xid);
        msgBuffer.add(msg);
        bufferTrack++;
        */



        //List<OFMessage> msglist = new ArrayList<OFMessage>(1);
        msg.setXid(xid);
        synchronized( flushableMsgBuffer ) {
            flushableMsgBuffer.add(msg);
        }

        trafficStatsHandler.countForEntitySimpleMeasurement(switchChannelID,
                TrafficStatisticsHandler.ENTITY_COUNTER_SND_MSG);

        //this.channel.write(msglist);

        /*
        if (msg.getType() == OFType.FLOW_MOD){
            this.trafficStatsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.FLOW_MOD_SENT);
            this.trafficStatsHandler.countForRateMeasurement(TrafficStatisticsHandler.FLOW_MOD_SENT);
        }
        */


        return xid;
    }


    @Override
    public Integer asyncFastSend(OFMessage msg) {
        return asyncFastSend(msg, getNextXid());
    }

    @Override
    public Integer asyncFastSend(OFMessage msg, int xid) {
        msg.setXid(xid);
        List<OFMessage> msglist = new ArrayList<OFMessage>(1);
        msglist.add(msg);
        this.channel.write(msglist);
        trafficStatsHandler.countForEntitySimpleMeasurement(switchChannelID,
                TrafficStatisticsHandler.ENTITY_COUNTER_SND_MSG);
        return xid;
    }

    @Override
    public Object syncSend(OFMessage msg) {
        int xid = getNextXid();
        return syncSend(msg, xid);
    }

    private Object syncSend(OFMessage msg, int xid) {
        return syncMessageInternal(msg, xid, true);
    }

    @Override
    public Map<Short, OFPhysicalPort> getPhysicalPorts() {
        return this.physicalPorts;
    }

    @Override
    public Set<Short> getPorts() {
        return this.physicalPorts.keySet();
    }

    @Override
    public OFPhysicalPort getPhysicalPort(Short portNumber) {
        return this.physicalPorts.get(portNumber);
    }

    @Override
    public Integer getPortBandwidth(Short portNumber) {
        return this.portBandwidth.get(portNumber);
    }

    @Override
    public boolean isPortEnabled(short portNumber) {
        return isPortEnabled(physicalPorts.get(portNumber));
    }

    @Override
    public boolean isPortEnabled(OFPhysicalPort port) {
        if (port == null) {
            return false;
        }
        int portConfig = port.getConfig();
        int portState = port.getState();
        if ((portConfig & OFPortConfig.OFPPC_PORT_DOWN.getValue()) > 0) {
            return false;
        }
        if ((portState & OFPortState.OFPPS_LINK_DOWN.getValue()) > 0) {
            return false;
        }
        if ((portState & OFPortState.OFPPS_STP_MASK.getValue()) == OFPortState.OFPPS_STP_BLOCK
                .getValue()) {
            return false;
        }
        return true;

    }

    @Override
    public List<OFPhysicalPort> getEnabledPorts() {
        List<OFPhysicalPort> result = new ArrayList<OFPhysicalPort>();
        synchronized (this.physicalPorts) {
            for (OFPhysicalPort port : physicalPorts.values()) {
                if (isPortEnabled(port)) {
                    result.add(port);
                }
            }
        }
        return result;
    }


    /**
     * WARNING: CALLER WOULD BE BLOCKED
     *
     */
    @Override
    public Object getStatistics(OFStatisticsRequest req) {
        int xid = getNextXid();
        StatisticsCollector worker = new StatisticsCollector(this, xid, req);
        messageWaitingDone.put(xid, worker);
        Future<Object> submit = executor.submit(worker);
        Object result = null;
        try {
            result = submit.get(responseTimerValue, TimeUnit.MILLISECONDS);
            return result;
        } catch (Exception e) {
            logger.warn("Timeout while waiting for {} replies", req.getType());
            result = null; // to indicate timeout has occurred
            return result;
        }
    }

    @Override
    public boolean isOperational() {
        return ((this.state == SwitchState.WAIT_CONFIG_REPLY) || (this.state == SwitchState.OPERATIONAL));
    }

    @Override
    public Object syncSendBarrierMessage() {
        OFBarrierRequest barrierMsg = new OFBarrierRequest();
        return syncSend(barrierMsg);
    }

    @Override
    public Object asyncSendBarrierMessage() {
        List<OFMessage> msglist = new ArrayList<OFMessage>(1);
        OFBarrierRequest barrierMsg = new OFBarrierRequest();
        int xid = getNextXid();

        barrierMsg.setXid(xid);
        msglist.add(barrierMsg);

        this.channel.write(msglist);
        trafficStatsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.BARRIER_REQUEST_SENT);
        return Boolean.TRUE;
    }


    @Override
    public void handleMessage(OFMessage ofMessage) {


        logger.debug("Message received: {}", ofMessage.toString());
        this.lastMsgReceivedTimeStamp = System.currentTimeMillis();
        OFType type = ofMessage.getType();
        switch (type) {
        case HELLO:
            logger.debug("<<<< HELLO");
            // send feature request
            trafficStatsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.HELLO_RECEIVED);
            OFMessage featureRequest = factory
                    .getMessage(OFType.FEATURES_REQUEST);
            asyncFastSend(featureRequest);
            trafficStatsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.FEATURES_REQUEST_SENT);
            // delete all pre-existing flows
            OFMatch match = new OFMatch().setWildcards(OFMatch.OFPFW_ALL);
            OFFlowMod flowMod = (OFFlowMod) factory
                    .getMessage(OFType.FLOW_MOD);
            flowMod.setMatch(match).setCommand(OFFlowMod.OFPFC_DELETE)
                    .setOutPort(OFPort.OFPP_NONE)
                    .setLength((short) OFFlowMod.MINIMUM_LENGTH);
            asyncFastSend(flowMod);
            this.state = SwitchState.WAIT_FEATURES_REPLY;
            if (START_LIVELINESS_TIMER){
                startSwitchTimer();
            }
            break;
        case ECHO_REQUEST:
            logger.debug("<<<< ECHO REQUEST");
            trafficStatsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.ECHO_REQUEST_RECEIVED);
            OFEchoReply echoReply = (OFEchoReply) factory
                    .getMessage(OFType.ECHO_REPLY);
            asyncFastSend(echoReply);
            trafficStatsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.ECHO_REPLY_SENT);

            break;
        case ECHO_REPLY:
            logger.debug("<<<< ECHO REPLY");
            trafficStatsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.ECHO_REPLY_RECEIVED);
            //this.probeSent = false;
            break;
        case FEATURES_REPLY:
            logger.debug("<<<< FEATURES REPLY");
            trafficStatsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.FEATURES_REPLY_RECEIVED);
            processFeaturesReply((OFFeaturesReply) ofMessage);
            break;
        case GET_CONFIG_REPLY:
            logger.debug("<<<< CONFIG REPLY");
            // make sure that the switch can send the whole packet to the
            // controller
            trafficStatsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.CONFIG_REPLY_RECEIVED);
            if (((OFGetConfigReply) ofMessage).getMissSendLength() == (short) 0xffff) {
                this.state = SwitchState.OPERATIONAL;
            }
            break;
        case BARRIER_REPLY:
            logger.debug("<<<< BARRIER REPLY");
            trafficStatsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.BARRIER_REPLY_RECEIVED);
            processBarrierReply((OFBarrierReply) ofMessage);
            break;
        case ERROR:
            logger.debug("<<<< ERROR");
            trafficStatsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.ERROR_MSG_RECEIVED);
            processErrorReply((OFError) ofMessage);
            break;
        case PORT_STATUS:
            logger.debug("<<<< PORT STATUS");
            trafficStatsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.PORT_STATUS_RECEIVED);
            processPortStatusMsg((OFPortStatus) ofMessage);
            break;
        case STATS_REPLY:
            logger.debug("<<<< STATS REPLY");
            trafficStatsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.STATS_RESPONSE_RECEIVED);
            processStatsReply((OFStatisticsReply) ofMessage);
            break;
        case PACKET_IN:
            logger.debug("<<<< PACKET_IN");
            trafficStatsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.PACKET_IN_RECEIVED);
            trafficStatsHandler.countForRateMeasurement(TrafficStatisticsHandler.PACKET_IN_RECEIVED);
            break;
        default:
            break;
        } // end of switch
        if (isOperational()) {
            logger.debug("SWITCH IS OPERATIONAL ... forwarding");
            SwitchEvent ev = new SwitchEvent(
                    SwitchEvent.SwitchEventType.SWITCH_MESSAGE, this, ofMessage);
            controller.switchMessage(ev, switchChannelID);
        }
    }


    private void startSwitchTimer(){
        if (this.timer != null){
            if (switchLivelinessTask == null){
                switchLivelinessTask = new SwitchLivelinessTimerTask();
            }
            switchLivelinessTaskHandle = timer.newTimeout(switchLivelinessTask,
                    switchLivenessTimeout, TimeUnit.SECONDS);
        }
    }



    /**
     * This method returns the switch liveness timeout value. If controller did
     * not receive any message from the switch for such a long period,
     * controller will tear down the connection to the switch.
     *
     * @return The timeout value
     */
    private static int getSwitchLivenessTimeout() {
        String timeout = System.getProperty("of.switchLivenessTimeout");
        int rv = 60500;
        try {
            if (timeout != null) {
                rv = Integer.parseInt(timeout);
            }
        } catch (Exception e) {
        }
        return rv;
    }


    private void processFeaturesReply(OFFeaturesReply reply) {
        if (this.state == SwitchState.WAIT_FEATURES_REPLY) {
            this.sid = reply.getDatapathId();
            this.buffers = reply.getBuffers();
            this.capabilities = reply.getCapabilities();
            this.tables = reply.getTables();
            this.actions = reply.getActions();
            // notify core of this error event
            for (OFPhysicalPort port : reply.getPorts()) {
                updatePhysicalPort(port);
            }
            // config the switch to send full data packet
            OFSetConfig config = (OFSetConfig) factory
                    .getMessage(OFType.SET_CONFIG);
            config.setMissSendLength((short) 0xffff).setLengthU(
                    OFSetConfig.MINIMUM_LENGTH);
            asyncFastSend(config);
            // send config request to make sure the switch can handle the set
            // config
            OFMessage getConfig = factory.getMessage(OFType.GET_CONFIG_REQUEST);
            asyncFastSend(getConfig);
            trafficStatsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.CONFIG_REQUEST_SENT);
            this.state = SwitchState.WAIT_CONFIG_REPLY;
            // inform core that a new switch is now operational
            reportSwitchStateChange(true);
        }
    }


    private void updatePhysicalPort(OFPhysicalPort port) {
        Short portNumber = port.getPortNumber();
        physicalPorts.put(portNumber, port);
        portBandwidth
                .put(portNumber,
                        port.getCurrentFeatures()
                                & (OFPortFeatures.OFPPF_10MB_FD.getValue()
                                        | OFPortFeatures.OFPPF_10MB_HD
                                                .getValue()
                                        | OFPortFeatures.OFPPF_100MB_FD
                                                .getValue()
                                        | OFPortFeatures.OFPPF_100MB_HD
                                                .getValue()
                                        | OFPortFeatures.OFPPF_1GB_FD
                                                .getValue()
                                        | OFPortFeatures.OFPPF_1GB_HD
                                                .getValue() | OFPortFeatures.OFPPF_10GB_FD
                                            .getValue()));
        trafficStatsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.UPDATE_PHYSICAL_PORT);
    }


    private void reportSwitchStateChange(boolean added) {
        SwitchEvent ev = null;
        if (added) {
            ev = new SwitchEvent(SwitchEvent.SwitchEventType.SWITCH_ADD, this, null);
            controller.switchAdded(ev, switchChannelID);
        } else {
            ev = new SwitchEvent(SwitchEvent.SwitchEventType.SWITCH_DELETE, this, null);
            controller.switchDeleted(ev, switchChannelID);
        }
    }


    protected class SwitchLivelinessTimerTask implements TimerTask {

        @Override
        public void run(Timeout timeout) throws Exception {

            // set this reference in parent so that cancellation is
            // possible
            switchLivelinessTaskHandle = timeout;
            Long now = System.currentTimeMillis();
            if ((now - lastMsgReceivedTimeStamp) > switchLivenessTimeout) {
                if (state == SwitchState.WAIT_FEATURES_REPLY) {
                    // send another features request
                    OFMessage request = factory
                            .getMessage(OFType.FEATURES_REQUEST);
                    asyncFastSend(request);
                } else {
                    if (state == SwitchState.WAIT_CONFIG_REPLY) {
                        // send another config request
                        OFSetConfig config = (OFSetConfig) factory
                                .getMessage(OFType.SET_CONFIG);
                        config.setMissSendLength((short) 0xffff)
                        .setLengthU(OFSetConfig.MINIMUM_LENGTH);
                        asyncFastSend(config);
                        OFMessage getConfig = factory
                                .getMessage(OFType.GET_CONFIG_REQUEST);
                        asyncFastSend(getConfig);
                    }
                }
            }
            timer.newTimeout(this, switchLivenessTimeout, TimeUnit.SECONDS);

        }
    }


    /*
     * Either a BarrierReply or a OFError is received. If this is a reply for an
     * outstanding sync message, wake up associated task so that it can continue
     */
    private void processBarrierReply(OFBarrierReply msg) {
        Integer xid = msg.getXid();
        SynchronousMessage worker = (SynchronousMessage) messageWaitingDone
                .remove(xid);
        if (worker == null) {
            return;
        }
        worker.wakeup();
    }

    private void processErrorReply(OFError errorMsg) {
        try{
            OFMessage offendingMsg = errorMsg.getOffendingMsg();
            Integer xi = 0;
            if (offendingMsg != null) {
                xi = offendingMsg.getXid();
            } else {
                xi = errorMsg.getXid();
            }
        }
        catch(MessageParseException mpe){
            reportError(mpe);
        }
    }

    private void processPortStatusMsg(OFPortStatus msg) {
        OFPhysicalPort port = msg.getDesc();
        if (msg.getReason() == (byte) OFPortReason.OFPPR_MODIFY.ordinal()) {
            updatePhysicalPort(port);
        } else if (msg.getReason() == (byte) OFPortReason.OFPPR_ADD.ordinal()) {
            updatePhysicalPort(port);
        } else if (msg.getReason() == (byte) OFPortReason.OFPPR_DELETE
                .ordinal()) {
            deletePhysicalPort(port);
        }

    }

    private void deletePhysicalPort(OFPhysicalPort port) {
        Short portNumber = port.getPortNumber();
        physicalPorts.remove(portNumber);
        portBandwidth.remove(portNumber);
    }

    private void processStatsReply(OFStatisticsReply reply) {
        Integer xid = reply.getXid();
        StatisticsCollector worker = (StatisticsCollector) messageWaitingDone
                .get(xid);
        if (worker == null) {
            return;
        }
        if (worker.collect(reply)) {
            // if all the stats records are received (collect() returns true)
            // then we are done.
            messageWaitingDone.remove(xid);
            worker.wakeup();
        }
    }


    /**
     * This method performs synchronous operations for a given message. If
     * syncRequest is set to true, the message will be sent out followed by a
     * Barrier request message. Then it's blocked until the Barrier rely arrives
     * or timeout. If syncRequest is false, it simply skips the message send and
     * just waits for the response back.
     *
     * @param msg
     *            Message to be sent
     * @param xid
     *            Message XID
     * @param request
     *            If set to true, the message the message will be sent out
     *            followed by a Barrier request message. If set to false, it
     *            simply skips the sending and just waits for the Barrier reply.
     * @return the result
     */
    private Object syncMessageInternal(OFMessage msg, int xid, boolean syncRequest) {
        Object result = null;

        SynchronousMessage worker = new SynchronousMessage(this, xid, msg, syncRequest);
        messageWaitingDone.put(xid, worker);

        Boolean status = false;
        Future<Object> submit = executor.submit(worker);
        try {
            result = submit.get(responseTimerValue, TimeUnit.MILLISECONDS);
            messageWaitingDone.remove(xid);
            if (result == null) {
                // if result is null, then it means the switch can handle this
                // message successfully
                // convert the result into a Boolean with value true
                status = true;
                // logger.debug("Successfully send " +
                // msg.getType().toString());
                result = status;
            } else {
                // if result is not null, this means the switch can't handle
                // this message
                // the result if OFError already
                logger.debug("Send {} failed --> {}", msg.getType().toString(),
                        ((OFError) result).toString());
            }
            return result;
        } catch (Exception e) {
            logger.warn("Timeout while waiting for {} reply", msg.getType()
                    .toString());
            // convert the result into a Boolean with value false
            status = false;
            result = status;
            return result;
        }


    }


    private void sendFirstHello() {
        try {
            OFMessage msg = factory.getMessage(OFType.HELLO);
            asyncFastSend(msg);
            trafficStatsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.HELLO_SENT);
            trafficStatsHandler.addEntityForCounter(switchChannelID, TrafficStatisticsHandler.ENTITY_COUNTER_RCV_MSG);
            trafficStatsHandler.addEntityForCounter(switchChannelID, TrafficStatisticsHandler.ENTITY_COUNTER_SND_MSG);
        } catch (Exception e) {
            reportError(e);
        }
    }


    private void reportError(Exception e) {
        if (e instanceof AsynchronousCloseException
                || e instanceof InterruptedException
                || e instanceof SocketException || e instanceof IOException
                || e instanceof ClosedSelectorException) {
            logger.error("Caught exception {}", e.getMessage());
        } else {
            logger.error("Caught exception ", e);
        }
        // notify core of this error event and disconnect the switch

        // TODO: We do not need this because except-hanling is done by
        // Controller's OFChannelHandler

        /*
        SwitchEvent ev = new SwitchEvent(
                SwitchEvent.SwitchEventType.SWITCH_ERROR, this, null);

        controller.switchError(ev, switchChannelID);
        */
    }


    @Override
    public void flushBufferedMessages() {
        //flushBatchTrack++;
        //if (flushBatchTrack > BATCH_COUNT_FOR_FLUSHING){
        synchronized (flushableMsgBuffer) {
            if (flushableMsgBuffer.size() > 0){
                channel.write(flushableMsgBuffer);
                flushableMsgBuffer.clear();
            }
        }
        //    flushBatchTrack = 0;
        //}

    }

    @Override
    public SocketAddress getRemoteAddress() {
        return (channel != null) ? channel.getRemoteAddress() : null;
    }

    @Override
    public SocketAddress getLocalAddress() {
        return (channel != null) ? channel.getLocalAddress() : null;
    }

}
