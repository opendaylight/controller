
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.core.internal;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendaylight.controller.protocol_plugin.openflow.core.IController;
import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitch;
import org.openflow.protocol.OFBarrierReply;
import org.openflow.protocol.OFEchoReply;
import org.openflow.protocol.OFError;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFGetConfigReply;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPhysicalPort.OFPortConfig;
import org.openflow.protocol.OFPhysicalPort.OFPortFeatures;
import org.openflow.protocol.OFPhysicalPort.OFPortState;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFPortStatus;
import org.openflow.protocol.OFPortStatus.OFPortReason;
import org.openflow.protocol.OFSetConfig;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.OFType;
import org.openflow.protocol.factory.BasicFactory;
import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwitchHandler implements ISwitch {
    private static final Logger logger = LoggerFactory
            .getLogger(SwitchHandler.class);
    private static final int SWITCH_LIVENESS_TIMER = 5000;
    private static final int SWITCH_LIVENESS_TIMEOUT = 2 * SWITCH_LIVENESS_TIMER + 500;
    private static final int SYNCHRONOUS_FLOW_TIMEOUT = 2000;
    private static final int STATS_COLLECTION_TIMEOUT = 2000;
    private static final int bufferSize = 1024 * 1024;

    private String instanceName;
    private ISwitch thisISwitch;
    private IController core;
    private Long sid;
    private Integer buffers;
    private Integer capabilities;
    private Byte tables;
    private Integer actions;
    private Selector selector;
    private SelectionKey clientSelectionKey;
    private SocketChannel socket;
    private ByteBuffer inBuffer;
    private ByteBuffer outBuffer;
    private BasicFactory factory;
    private AtomicInteger xid;
    private SwitchState state;
    private Timer periodicTimer;
    private Map<Short, OFPhysicalPort> physicalPorts;
    private Map<Short, Integer> portBandwidth;
    private Date connectedDate;
    private Long lastMsgReceivedTimeStamp;
    private Boolean probeSent;
    private ExecutorService executor;
    private ConcurrentHashMap<Integer, Callable<Object>> messageWaitingDone;
    private boolean running;
    private Thread switchHandlerThread;

    private enum SwitchState {
        NON_OPERATIONAL(0), WAIT_FEATURES_REPLY(1), WAIT_CONFIG_REPLY(2), OPERATIONAL(
                3);

        private int value;

        private SwitchState(int value) {
            this.value = value;
        }

        @SuppressWarnings("unused")
        public int value() {
            return this.value;
        }
    }

    public SwitchHandler(Controller core, SocketChannel sc, String name) {
        this.instanceName = name;
        this.thisISwitch = this;
        this.sid = (long) 0;
        this.buffers = (int)0;
        this.capabilities = (int)0;
        this.tables = (byte)0;
        this.actions = (int)0;
        this.core = core;
        this.socket = sc;
        this.factory = new BasicFactory();
        this.connectedDate = new Date();
        this.lastMsgReceivedTimeStamp = connectedDate.getTime();
        this.physicalPorts = new HashMap<Short, OFPhysicalPort>();
        this.portBandwidth = new HashMap<Short, Integer>();
        this.state = SwitchState.NON_OPERATIONAL;
        this.probeSent = false;
        this.xid = new AtomicInteger(this.socket.hashCode());
        this.periodicTimer = null;
        this.executor = Executors.newFixedThreadPool(4);
        this.messageWaitingDone = new ConcurrentHashMap<Integer, Callable<Object>>();
        this.inBuffer = ByteBuffer.allocateDirect(bufferSize);
        this.outBuffer = ByteBuffer.allocateDirect(bufferSize);
    }

    public void start() {
        try {
            this.selector = SelectorProvider.provider().openSelector();
            this.socket.configureBlocking(false);
            this.socket.socket().setTcpNoDelay(true);
            this.clientSelectionKey = this.socket.register(this.selector,
                    SelectionKey.OP_READ);
            startHandlerThread();
        } catch (Exception e) {
            reportError(e);
            return;
        }
    }

    private void startHandlerThread() {
        OFMessage msg = factory.getMessage(OFType.HELLO);
        asyncSend(msg);
        switchHandlerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                running = true;
                while (running) {
                    try {
                        // wait for an incoming connection
                        selector.select(0);
                        Iterator<SelectionKey> selectedKeys = selector
                                .selectedKeys().iterator();
                        while (selectedKeys.hasNext()) {
                            SelectionKey skey = selectedKeys.next();
                            selectedKeys.remove();
                            if (skey.isValid() && skey.isWritable()) {
                                resumeSend();
                            }
                            if (skey.isValid() && skey.isReadable()) {
                                handleMessages();
                            }
                        }
                    } catch (Exception e) {
                    	reportError(e);
                    }
                }
            }
        }, instanceName);
        switchHandlerThread.start();
    }

    public void stop() {
        try {
            running = false;
            selector.wakeup();
            cancelSwitchTimer();
            this.clientSelectionKey.cancel();
            this.socket.close();
            executor.shutdown();
        } catch (Exception e) {
        	// do nothing since we are shutting down.
        	return;
        }
    }

    @Override
    public int getNextXid() {
        return this.xid.incrementAndGet();
    }

    @Override
    public Integer asyncSend(OFMessage msg) {
        return asyncSend(msg, getNextXid());
    }

    @Override
    public Integer asyncSend(OFMessage msg, int xid) {
        synchronized (outBuffer) {
            /*
            if ((msg.getType() != OFType.ECHO_REQUEST) &&
            		(msg.getType() != OFType.ECHO_REPLY)) {
            	logger.debug("sending " + msg.getType().toString() + " to " + toString());
            }
             */
            msg.setXid(xid);
            int msgLen = msg.getLengthU();
            if (outBuffer.remaining() < msgLen) {
                // increase the buffer size so that it can contain this message
                ByteBuffer newBuffer = ByteBuffer.allocateDirect(outBuffer
                        .capacity()
                        + msgLen);
                outBuffer.flip();
                newBuffer.put(outBuffer);
                outBuffer = newBuffer;
            }
            msg.writeTo(outBuffer);
            outBuffer.flip();
            try {
                socket.write(outBuffer);
                outBuffer.compact();
                if (outBuffer.position() > 0) {
                    this.clientSelectionKey = this.socket.register(
                            this.selector, SelectionKey.OP_WRITE, this);
                }
                logger.trace("Message sent: " + msg.toString());
            } catch (Exception e) {
                reportError(e);
            }
        }
        return xid;
    }

    public void resumeSend() {
        synchronized (outBuffer) {
            try {
                outBuffer.flip();
                socket.write(outBuffer);
                outBuffer.compact();
                if (outBuffer.position() > 0) {
                    this.clientSelectionKey = this.socket.register(
                            this.selector, SelectionKey.OP_WRITE, this);
                } else {
                    this.clientSelectionKey = this.socket.register(
                            this.selector, SelectionKey.OP_READ, this);
                }
            } catch (Exception e) {
                reportError(e);
            }
        }
    }

    public void handleMessages() {
        List<OFMessage> msgs = readMessages();
        if (msgs == null) {
            logger.debug(toString() + " is down");
            // the connection is down, inform core
            reportSwitchStateChange(false);
            return;
        }
        for (OFMessage msg : msgs) {
            logger.trace("Message received: " + msg.toString());
            /*
            if  ((msg.getType() != OFType.ECHO_REQUEST) &&
            		(msg.getType() != OFType.ECHO_REPLY)) {
            	logger.debug(msg.getType().toString() + " received from sw " + toString());
            }
             */
            this.lastMsgReceivedTimeStamp = System.currentTimeMillis();
            OFType type = msg.getType();
            switch (type) {
            case HELLO:
                // send feature request
                OFMessage featureRequest = factory
                        .getMessage(OFType.FEATURES_REQUEST);
                asyncSend(featureRequest);
                // delete all pre-existing flows
                OFMatch match = new OFMatch().setWildcards(OFMatch.OFPFW_ALL);
                OFFlowMod flowMod = (OFFlowMod) factory
                        .getMessage(OFType.FLOW_MOD);
                flowMod.setMatch(match).setCommand(OFFlowMod.OFPFC_DELETE)
                        .setOutPort(OFPort.OFPP_NONE).setLength(
                                (short) OFFlowMod.MINIMUM_LENGTH);
                asyncSend(flowMod);
                this.state = SwitchState.WAIT_FEATURES_REPLY;
                startSwitchTimer();
                break;
            case ECHO_REQUEST:
                OFEchoReply echoReply = (OFEchoReply) factory
                        .getMessage(OFType.ECHO_REPLY);
                asyncSend(echoReply);
                break;
            case ECHO_REPLY:
                this.probeSent = false;
                break;
            case FEATURES_REPLY:
                processFeaturesReply((OFFeaturesReply) msg);
                break;
            case GET_CONFIG_REPLY:
                // make sure that the switch can send the whole packet to the controller
                if (((OFGetConfigReply) msg).getMissSendLength() == (short) 0xffff) {
                    this.state = SwitchState.OPERATIONAL;
                }
                break;
            case BARRIER_REPLY:
                processBarrierReply((OFBarrierReply) msg);
                break;
            case ERROR:
                processErrorReply((OFError) msg);
                break;
            case PORT_STATUS:
                processPortStatusMsg((OFPortStatus) msg);
                break;
            case STATS_REPLY:
                processStatsReply((OFStatisticsReply) msg);
                break;
            case PACKET_IN:
                break;
            default:
                break;
            } // end of switch
            if (isOperational()) {
                ((Controller) core).takeSwitchEventMsg(thisISwitch, msg);
            }
        } // end of for
    }

    private void processPortStatusMsg(OFPortStatus msg) {
        //short portNumber = msg.getDesc().getPortNumber();
        OFPhysicalPort port = msg.getDesc();
        if (msg.getReason() == (byte) OFPortReason.OFPPR_MODIFY.ordinal()) {
            updatePhysicalPort(port);
            //logger.debug("Port " + portNumber + " on " + toString() + " modified");
        } else if (msg.getReason() == (byte) OFPortReason.OFPPR_ADD.ordinal()) {
            updatePhysicalPort(port);
            //logger.debug("Port " + portNumber + " on " + toString() + " added");
        } else if (msg.getReason() == (byte) OFPortReason.OFPPR_DELETE
                .ordinal()) {
            deletePhysicalPort(port);
            //logger.debug("Port " + portNumber + " on " + toString() + " deleted");
        }

    }

    private List<OFMessage> readMessages() {
        List<OFMessage> msgs = null;
        int bytesRead;
        try {
            bytesRead = socket.read(inBuffer);
        } catch (Exception e) {
            reportError(e);
            return null;
        }
        if (bytesRead == -1) {
            return null;
        }
        inBuffer.flip();
        msgs = factory.parseMessages(inBuffer);
        if (inBuffer.hasRemaining()) {
            inBuffer.compact();
        } else {
            inBuffer.clear();
        }
        return msgs;
    }

    private void startSwitchTimer() {
        this.periodicTimer = new Timer();
        this.periodicTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    Long now = System.currentTimeMillis();
                    if ((now - lastMsgReceivedTimeStamp) > SWITCH_LIVENESS_TIMEOUT) {
                        if (probeSent) {
                            // switch failed to respond to our probe, consider it down
                            logger.warn(toString()
                                    + " is idle for too long, disconnect");
                            reportSwitchStateChange(false);
                        } else {
                            // send a probe to see if the switch is still alive
                            //logger.debug("Send idle probe (Echo Request) to " + switchName());
                            probeSent = true;
                            OFMessage echo = factory
                                    .getMessage(OFType.ECHO_REQUEST);
                            asyncSend(echo);
                        }
                    } else {
                        if (state == SwitchState.WAIT_FEATURES_REPLY) {
                            // send another features request
                            OFMessage request = factory
                                    .getMessage(OFType.FEATURES_REQUEST);
                            asyncSend(request);
                        } else {
                            if (state == SwitchState.WAIT_CONFIG_REPLY) {
                                //  send another config request
                                OFSetConfig config = (OFSetConfig) factory
                                        .getMessage(OFType.SET_CONFIG);
                                config.setMissSendLength((short) 0xffff)
                                        .setLengthU(OFSetConfig.MINIMUM_LENGTH);
                                asyncSend(config);
                                OFMessage getConfig = factory
                                        .getMessage(OFType.GET_CONFIG_REQUEST);
                                asyncSend(getConfig);
                            }
                        }
                    }
                } catch (Exception e) {
                    reportError(e);
                }
            }
        }, SWITCH_LIVENESS_TIMER, SWITCH_LIVENESS_TIMER);
    }

    private void cancelSwitchTimer() {
        if (this.periodicTimer != null) {
            this.periodicTimer.cancel();
        }
    }

    private void reportError(Exception e) {
        //logger.error(toString() + " caught Error " + e.toString());
        // notify core of this error event
        ((Controller) core).takeSwitchEventError(this);
    }

    private void reportSwitchStateChange(boolean added) {
        if (added) {
            ((Controller) core).takeSwtichEventAdd(this);
        } else {
            ((Controller) core).takeSwitchEventDelete(this);
        }
    }

    @Override
    public Long getId() {
        return this.sid;
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
            asyncSend(config);
            // send config request to make sure the switch can handle the set config
            OFMessage getConfig = factory.getMessage(OFType.GET_CONFIG_REQUEST);
            asyncSend(getConfig);
            this.state = SwitchState.WAIT_CONFIG_REPLY;
            // inform core that a new switch is now operational
            reportSwitchStateChange(true);
        }
    }

    private void updatePhysicalPort(OFPhysicalPort port) {
        Short portNumber = port.getPortNumber();
        physicalPorts.put(portNumber, port);
        portBandwidth
                .put(
                        portNumber,
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
    }

    private void deletePhysicalPort(OFPhysicalPort port) {
        Short portNumber = port.getPortNumber();
        physicalPorts.remove(portNumber);
        portBandwidth.remove(portNumber);
    }

    @Override
    public boolean isOperational() {
        return ((this.state == SwitchState.WAIT_CONFIG_REPLY) || (this.state == SwitchState.OPERATIONAL));
    }

    @Override
    public String toString() {
        return ("["
                + this.socket.toString()
                + " SWID "
                + (isOperational() ? HexString.toHexString(this.sid)
                        : "unkbown") + "]");
    }

    @Override
    public Date getConnectedDate() {
        return this.connectedDate;
    }

    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public Object getStatistics(OFStatisticsRequest req) {
        int xid = getNextXid();
        StatisticsCollector worker = new StatisticsCollector(this, xid, req);
        messageWaitingDone.put(xid, worker);
        Future<Object> submit = executor.submit(worker);
        Object result = null;
        try {
            result = submit
                    .get(STATS_COLLECTION_TIMEOUT, TimeUnit.MILLISECONDS);
            return result;
        } catch (Exception e) {
            logger.warn("Timeout while waiting for " + req.getType()
                    + " replies");
            result = null; // to indicate timeout has occurred
            return result;
        }
    }

    @Override
    public Object syncSend(OFMessage msg) {
        Integer xid = getNextXid();
        SynchronousMessage worker = new SynchronousMessage(this, xid, msg);
        messageWaitingDone.put(xid, worker);
        Object result = null;
        Boolean status = false;
        Future<Object> submit = executor.submit(worker);
        try {
            result = submit
                    .get(SYNCHRONOUS_FLOW_TIMEOUT, TimeUnit.MILLISECONDS);
            messageWaitingDone.remove(xid);
            if (result == null) {
                // if result  is null, then it means the switch can handle this message successfully
                // convert the result into a Boolean with value true
                status = true;
                //logger.debug("Successfully send " + msg.getType().toString());
                result = status;
            } else {
                // if result  is not null, this means the switch can't handle this message
                // the result if OFError already
                logger.debug("Send " + msg.getType().toString()
                        + " failed --> " + ((OFError) result).toString());
            }
            return result;
        } catch (Exception e) {
            logger.warn("Timeout while waiting for " + msg.getType().toString()
                    + " reply");
            // convert the result into a Boolean with value false
            status = false;
            result = status;
            return result;
        }
    }

    /*
     * Either a BarrierReply or a OFError is received. If this is a reply for an outstanding sync message,
     * wake up associated task so that it can continue
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
        OFMessage offendingMsg = errorMsg.getOffendingMsg();
        Integer xid;
        if (offendingMsg != null) {
            xid = offendingMsg.getXid();
        } else {
            xid = errorMsg.getXid();
        }
        /*
         * the error can be a reply to a synchronous message or to a statistic request message
         */
        Callable<?> worker = messageWaitingDone.remove(xid);
        if (worker == null) {
            return;
        }
        if (worker instanceof SynchronousMessage) {
            ((SynchronousMessage) worker).wakeup(errorMsg);
        } else {
            ((StatisticsCollector) worker).wakeup(errorMsg);
        }
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

    @Override
    public Map<Short, OFPhysicalPort> getPhysicalPorts() {
        return this.physicalPorts;
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
    public Set<Short> getPorts() {
        return this.physicalPorts.keySet();
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
}
