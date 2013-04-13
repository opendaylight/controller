
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.core.internal;

import java.io.IOException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Comparator;
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
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendaylight.controller.protocol_plugin.openflow.core.IController;
import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitch;
import org.opendaylight.controller.protocol_plugin.openflow.core.IMessageReadWrite;
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
    private int MESSAGE_RESPONSE_TIMER = 2000;

    private String instanceName;
    private ISwitch thisISwitch;
    private IController core;
    private Long sid;
    private Integer buffers;
    private Integer capabilities;
    private Byte tables;
    private Integer actions;
    private Selector selector;
    private SocketChannel socket;
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
    private IMessageReadWrite msgReadWriteService;
    private Thread switchHandlerThread;
    private Integer responseTimerValue;
	private PriorityBlockingQueue<PriorityMessage> transmitQ;
    private Thread transmitThread;
    
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
        this.responseTimerValue = MESSAGE_RESPONSE_TIMER;
        String rTimer = System.getProperty("of.messageResponseTimer");
        if (rTimer != null) {
        	try {
        		responseTimerValue = Integer.decode(rTimer);
        	} catch (NumberFormatException e) {
				logger.warn("Invalid of.messageResponseTimer: {} use default({})",
						rTimer, MESSAGE_RESPONSE_TIMER);
        	}
        }
	}

    public void start() {
        try {
        	startTransmitThread();
        	setupCommChannel();
        	sendFirstHello();
            startHandlerThread();
        } catch (Exception e) {
            reportError(e);
        }
    }

    private void startHandlerThread() {
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
    	running = false;
    	cancelSwitchTimer();
    	try {
    		selector.wakeup();
    		selector.close();
		} catch (Exception e) {
		}
    	try {
			socket.close();
		} catch (Exception e) {
		}
    	try {
			msgReadWriteService.stop();
		} catch (Exception e) {
		}
    	executor.shutdown();
    	
    	selector = null;
    	socket = null;
		msgReadWriteService = null;
		
		if (switchHandlerThread != null) {
			switchHandlerThread.interrupt();
		}
		if (transmitThread != null) {
			transmitThread.interrupt();
		}
    }

    @Override
    public int getNextXid() {
        return this.xid.incrementAndGet();
    }

	/**
	 * This method puts the message in an outgoing priority queue with normal
	 * priority. It will be served after high priority messages. The method
	 * should be used for non-critical messages such as statistics request,
	 * discovery packets, etc. An unique XID is generated automatically and
	 * inserted into the message.
	 * 
	 * @param msg The OF message to be sent
	 * @return The XID used
	 */
    @Override
    public Integer asyncSend(OFMessage msg) {
    	return asyncSend(msg, getNextXid());
    }

	/**
	 * This method puts the message in an outgoing priority queue with normal
	 * priority. It will be served after high priority messages. The method
	 * should be used for non-critical messages such as statistics request,
	 * discovery packets, etc. The specified XID is inserted into the message.
	 * 
	 * @param msg The OF message to be Sent
	 * @param xid The XID to be used in the message
	 * @return The XID used
	 */
    @Override
    public Integer asyncSend(OFMessage msg, int xid) {
    	msg.setXid(xid);
    	transmitQ.add(new PriorityMessage(msg, 0));
        return xid;
    }

	/**
	 * This method puts the message in an outgoing priority queue with high
	 * priority. It will be served first before normal priority messages. The
	 * method should be used for critical messages such as hello, echo reply
	 * etc. An unique XID is generated automatically and inserted into the
	 * message.
	 * 
	 * @param msg The OF message to be sent
	 * @return The XID used
	 */
    @Override
    public Integer asyncFastSend(OFMessage msg) {
    	return asyncFastSend(msg, getNextXid());
    }

	/**
	 * This method puts the message in an outgoing priority queue with high
	 * priority. It will be served first before normal priority messages. The
	 * method should be used for critical messages such as hello, echo reply
	 * etc. The specified XID is inserted into the message.
	 * 
	 * @param msg The OF message to be sent
	 * @return The XID used
	 */
    @Override
    public Integer asyncFastSend(OFMessage msg, int xid) {
    	msg.setXid(xid);
    	transmitQ.add(new PriorityMessage(msg, 1));
        return xid;
    }

   public void resumeSend() {
        try {
			msgReadWriteService.resumeSend();
		} catch (Exception e) {
			reportError(e);
		}
    }

    public void handleMessages() {
        List<OFMessage> msgs = null;
        
        try {
        	msgs = msgReadWriteService.readMessages();
		} catch (Exception e) {
			reportError(e);
		}
		
        if (msgs == null) {
            logger.debug("{} is down", toString());
            // the connection is down, inform core
            reportSwitchStateChange(false);
            return;
        }
        for (OFMessage msg : msgs) {
            logger.trace("Message received: {}", msg.toString());
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
                asyncFastSend(featureRequest);
                // delete all pre-existing flows
                OFMatch match = new OFMatch().setWildcards(OFMatch.OFPFW_ALL);
                OFFlowMod flowMod = (OFFlowMod) factory
                        .getMessage(OFType.FLOW_MOD);
                flowMod.setMatch(match).setCommand(OFFlowMod.OFPFC_DELETE)
                        .setOutPort(OFPort.OFPP_NONE).setLength(
                                (short) OFFlowMod.MINIMUM_LENGTH);
                asyncFastSend(flowMod);
                this.state = SwitchState.WAIT_FEATURES_REPLY;
                startSwitchTimer();
                break;
            case ECHO_REQUEST:
                OFEchoReply echoReply = (OFEchoReply) factory
                        .getMessage(OFType.ECHO_REPLY);
                asyncFastSend(echoReply);
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
                            logger.warn("{} is idle for too long, disconnect", toString());
                            reportSwitchStateChange(false);
                        } else {
                            // send a probe to see if the switch is still alive
                            //logger.debug("Send idle probe (Echo Request) to " + switchName());
                            probeSent = true;
                            OFMessage echo = factory
                                    .getMessage(OFType.ECHO_REQUEST);
                            asyncFastSend(echo);
                        }
                    } else {
                        if (state == SwitchState.WAIT_FEATURES_REPLY) {
                            // send another features request
                            OFMessage request = factory
                                    .getMessage(OFType.FEATURES_REQUEST);
                            asyncFastSend(request);
                        } else {
                            if (state == SwitchState.WAIT_CONFIG_REPLY) {
                                //  send another config request
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
    	if (e instanceof AsynchronousCloseException) {
    		logger.debug("Caught exception {}", e.getMessage());
    	} else {
    		logger.warn("Caught exception {}", e.getMessage());
    	}
        // notify core of this error event and disconnect the switch
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
            asyncFastSend(config);
            // send config request to make sure the switch can handle the set config
            OFMessage getConfig = factory.getMessage(OFType.GET_CONFIG_REQUEST);
            asyncFastSend(getConfig);
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
                    .get(MESSAGE_RESPONSE_TIMER, TimeUnit.MILLISECONDS);
            return result;
        } catch (Exception e) {
            logger.warn("Timeout while waiting for {} replies", req.getType());
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
                    .get(responseTimerValue, TimeUnit.MILLISECONDS);
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
                logger.debug("Send {} failed --> {}", 
                		msg.getType().toString(), ((OFError) result).toString());
            }
            return result;
        } catch (Exception e) {
            logger.warn("Timeout while waiting for {} reply", msg.getType().toString());
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

	/*
	 * Transmit thread polls the message out of the priority queue and invokes
	 * messaging service to transmit it over the socket channel
	 */
    class PriorityMessageTransmit implements Runnable {
        public void run() {
            running = true;
            while (running) {
            	try {
            		if (!transmitQ.isEmpty()) {
            			PriorityMessage pmsg = transmitQ.poll();
            			msgReadWriteService.asyncSend(pmsg.msg);
            			logger.trace("Message sent: {}", pmsg.toString());
            		}
            		Thread.sleep(10);
            	} catch (Exception e) {
            		reportError(e);
            	}
            }
        	transmitQ = null;
        }
    }

    /*
     * Setup and start the transmit thread
     */
    private void startTransmitThread() {    	
        this.transmitQ = new PriorityBlockingQueue<PriorityMessage>(11, 
				new Comparator<PriorityMessage>() {
					public int compare(PriorityMessage p1, PriorityMessage p2) {
						return p2.priority - p1.priority;
					}
				});
        this.transmitThread = new Thread(new PriorityMessageTransmit());
        this.transmitThread.start();
    }
    
    /*
     * Setup communication services
     */
    private void setupCommChannel() throws Exception {
        this.selector = SelectorProvider.provider().openSelector();
        this.socket.configureBlocking(false);
        this.socket.socket().setTcpNoDelay(true);        
        this.msgReadWriteService = getMessageReadWriteService();
    }

    private void sendFirstHello() {
    	try {
    		OFMessage msg = factory.getMessage(OFType.HELLO);
    		asyncFastSend(msg);
    	} catch (Exception e) {
    		reportError(e);
    	}
    }
    
    private IMessageReadWrite getMessageReadWriteService() throws Exception {
    	String str = System.getProperty("secureChannelEnabled");
        return ((str != null) && (str.trim().equalsIgnoreCase("true"))) ? 
        		new SecureMessageReadWriteService(socket, selector) : 
        		new MessageReadWriteService(socket, selector);
    }
}
