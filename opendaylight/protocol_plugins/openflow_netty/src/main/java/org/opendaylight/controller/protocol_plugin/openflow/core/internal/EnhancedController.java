package org.opendaylight.controller.protocol_plugin.openflow.core.internal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.RejectedExecutionException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.AdaptiveReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelUpstreamHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.ObjectSizeEstimator;
import org.jboss.netty.handler.timeout.ReadTimeoutException;




import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.openflow.protocol.factory.BasicFactory;
import org.openflow.protocol.factory.MessageParseException;


import org.opendaylight.controller.protocol_plugin.openflow.core.IController;
import org.opendaylight.controller.protocol_plugin.openflow.core.IMessageListener;
import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitch;
import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitchStateListener;
//import org.opendaylight.controller.protocol_plugin.openflow.core.internal.OFChannelState.HandshakeState;
//import org.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class EnhancedController implements IController {


    protected BasicFactory factory;


    private static final Logger logger = LoggerFactory
            .getLogger(EnhancedController.class);


    // Track connected switches via SwitchID
    private ConcurrentHashMap<Long, ISwitch> connectedSwitches;

    // Track connected switches via ChannelID. Whenever the message
    private ConcurrentHashMap<Integer, ISwitch> channelIDToSwitchMap;

    // only 1 message listener per OFType
    private ConcurrentMap<OFType, IMessageListener> messageListeners;

    // only 1 switch state listener
    private ISwitchStateListener switchStateListener;
    private AtomicInteger switchInstanceNumber;


    private OFChannelHandler ofChannelHandler = null;
    private ControllerServerBootstrap bootstrap = null;

    private ThreadPoolExecutor execHandler = null;

    private static final int SEND_BUFFER_SIZE = 1 * 1024 * 1024;
    private static final int RECEIVE_BUFFER_SIZE = 1 * 1024 * 1024;
    private static final int WRITE_BUFFER_LOW_WATERMARK = 32 * 1024;
    private static final int WRITE_BUFFER_HIGH_WATERMARK = 64 * 1024;
    private static final String CONTROLLER_HOST = null;
    private static final int CONTROLLER_PORT = 6633;

    private static final int OMATPE_CORE_POOL_SIZE = 200;
    private static final int OMATPE_PER_CHANNEL_SIZE = 2 * 1048576;
    private static final int OMATPE_POOL_WIDE_SIZE = 0; //1073741824;
    private static final int OMATPE_THREAD_KEEP_ALIVE_IN_MILLISECONDS = 100;
    private static final int EXPERIMENTAL_OMATPE_OBJECT_SIZE = 1000; // bytes

    private HashedWheelTimer hashedWheelTimer = null;

    // This executor would be used by individual switches to handle
    // cases like Stats Request/Response or Sync* methods which sends request and
    // waits via Future for responses. Please note that threads in this
    // pool are shared across multiple threads. So, if all threads are busy,
    // Socket IO thread would get blocked creating sharp decline in performance
    // If possible TOTALLY avoid any thread usage which does network level
    // request / response by making a thread in this pool wait for response
    // Consider storing the Future reference against the "sent" request and
    // fire-event to wake-up the same when response is received rather than making the
    // sender thread getting into a "wait" mode. That would never scale
    private ExecutorService executorService = null;

    // IMPORTANT: DO NOT REDUCE THIS THREAD COUNT TO 0
    // THIS THREAD COUNT WOULD BE USED FOR SOCKET-IO + FOLLOWING EXECUTION CHAIN
    // Plugin + SAL + North-to-SAL + Egress (flow_provisioning)
    private static final int WORKER_THREAD_COUNT = 4;

    // This is a handy thread-pool if WORKER_THREAD_COUNT is not able to cope with
    // Socket IO + Execution of the following handling chain
    // Plugin + SAL + North-to-SAL + Egress (flow_provisioning)
    private static final int EXECUTION_HANDLER_THREAD_POOL_SIZE = 0;

    // This is the thread-pool which can be optionally used for
    // building synchronous semantics for flow_mod and stats handling cycle
    // Flow_Mod in synchronous model could involve FLOW_MOD + BARRIER_MSG
    // sending and receiving with wait timeout for reply
    // Stats handling in synchronous model could involve STATS_REQUEST + STATS_REPLY
    // sending and receiving with wait timeout for reply
    private static final int THREAD_POOL_SIZE_FOR_EGRESS_SYNC_MSGS = 30;

    private TrafficStatisticsHandler statsHandler = null;

    // Lock for locking messagelisteners list while escalating the switch
    // messages
    private ReentrantLock lock = new ReentrantLock();

    private static final int FLUSH_BATCH_SIZE = 100;

    //****************** IController Interafce Methods Begin ******************

    @Override
    public void addMessageListener(OFType type, IMessageListener listener) {
        IMessageListener currentListener = this.messageListeners.get(type);
        if (currentListener != null) {
            logger.warn("{} is already listened by {}", type.toString(),
                    currentListener.toString());
        }
        this.messageListeners.put(type, listener);
        logger.debug("{} is now listened by {}", type.toString(),
                listener.toString());

    }

    @Override
    public void removeMessageListener(OFType type, IMessageListener listener) {
        IMessageListener currentListener = this.messageListeners.get(type);
        if ((currentListener != null) && (currentListener == listener)) {
            logger.debug("{} listener {} is Removed", type.toString(),
                    listener.toString());
            this.messageListeners.remove(type);
        }

    }

    @Override
    public void addSwitchStateListener(ISwitchStateListener listener) {
        if (this.switchStateListener != null) {
            logger.warn("Switch events are already listened by {}",
                    this.switchStateListener.toString());
        }
        this.switchStateListener = listener;
        logger.debug("Switch events are now listened by {}",
                listener.toString());

    }

    @Override
    public void removeSwitchStateListener(ISwitchStateListener listener) {
        if ((this.switchStateListener != null)
                && (this.switchStateListener == listener)) {
            logger.debug("SwitchStateListener {} is Removed",
                    listener.toString());
            this.switchStateListener = null;
        }

    }

    @Override
    public Map<Long, ISwitch> getSwitches() {
        return this.connectedSwitches;
    }

    @Override
    public ISwitch getSwitch(Long switchId) {
        return this.connectedSwitches.get(switchId);
    }

    //****************** IController Interafce Methods End ******************



    //****************** Dependency-manager callbacks Begin ******************
    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    public void init() {
        logger.debug("Initializing!");
        this.connectedSwitches = new ConcurrentHashMap<Long, ISwitch>();
        this.channelIDToSwitchMap = new ConcurrentHashMap<Integer, ISwitch>();
        this.messageListeners = new ConcurrentHashMap<OFType, IMessageListener>();
        this.switchStateListener = null;
        this.hashedWheelTimer = new HashedWheelTimer();
        this.statsHandler = new TrafficStatisticsHandler(hashedWheelTimer);
        this.switchInstanceNumber = new AtomicInteger(0);
        this.factory = new BasicFactory();
        this.bootstrap = new ControllerServerBootstrap(this);
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE_FOR_EGRESS_SYNC_MSGS);


    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    public void start() {
        this.statsHandler.init();
        logger.debug("Starting!");
        bootstrap.startServer(WORKER_THREAD_COUNT,
                CONTROLLER_HOST,
                CONTROLLER_PORT,
                ofChannelHandler);


    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    public void stop() {
        for (Iterator<Entry<Integer, ISwitch>> it = channelIDToSwitchMap.entrySet().iterator(); it
                .hasNext();) {
            Entry<Integer, ISwitch> entry = it.next();
            ((EnhancedSwitchHandler) entry.getValue()).stop();
        }

        hashedWheelTimer.stop();

        executorService.shutdown();
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    public void destroy() {
    }
    //****************** Dependency-manager callbacks End ******************



    public OFChannelHandler getChannelHandler(){
        return new OFChannelHandler(this);
    }


    protected class OFChannelHandler extends IdleStateAwareChannelUpstreamHandler{


        protected EnhancedController controller = null;
        protected Channel channel = null;


        public OFChannelHandler(EnhancedController controller){
            this.controller = controller;
        }


        @Override
        public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e)
                throws Exception {
            List<OFMessage> msglist = new ArrayList<OFMessage>(1);
            msglist.add(factory.getMessage(OFType.ECHO_REQUEST));
            e.getChannel().write(msglist);
            statsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.ECHO_REQUEST_SENT);
        }

        @Override
        public void channelConnected(ChannelHandlerContext ctx,
                ChannelStateEvent e) throws Exception {
            channel = e.getChannel();
            logger.info("New switch connection from {}",
                     channel.getRemoteAddress());

            Integer channelID = e.getChannel().getId();

            ISwitch switchHandler = new EnhancedSwitchHandler(controller,
                    channelID, channel, hashedWheelTimer, executorService, statsHandler);
            switchHandler.startHandler();
            channelIDToSwitchMap.put(channelID, switchHandler);
            statsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.CONNECTED_SWITCHES);

          }

        @Override
        public void channelDisconnected(ChannelHandlerContext ctx,
                ChannelStateEvent e) throws Exception {
            // when SwitchHandler.shutDownHandler is called, Controller would
            // get the feedback via switchDeleted method. So that both SwitchHandler and
            // controller both release resources of the switch concerned

            Integer channelID = e.getChannel().getId();
            ISwitch switchHandler = channelIDToSwitchMap.get(channelID);
            if (switchHandler != null){
                switchHandler.shutDownHandler();
            }
            statsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.DISCONNECTED_SWITCHES);

        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
                throws Exception {

            EnhancedSwitchHandler sw = null;

            if (e.getCause() instanceof ReadTimeoutException) {
                // switch timeout
                logger.error("Disconnecting switch {} due to read timeout",
                        e.getChannel().getId(), e.getCause().getMessage());
                ctx.getChannel().close();
                sw = (EnhancedSwitchHandler)channelIDToSwitchMap.get(e.getChannel().getId());
                sw.stop();
            /*
            } else if (e.getCause() instanceof HandshakeTimeoutException) {
                logger.error("Disconnecting switch {}: failed to complete handshake",
                        e.getChannel().getId());
                ctx.getChannel().close();
                channelIDToSwitchMap.remove(e.getChannel().getId());
                */
            } else if (e.getCause() instanceof ClosedChannelException) {
                logger.warn("Channel for sw {} already closed Error : {}",
                        e.getChannel().getId(), e.getCause().getMessage());
                ctx.getChannel().close();
                sw = (EnhancedSwitchHandler)channelIDToSwitchMap.get(e.getChannel().getId());
                sw.stop();
            } else if (e.getCause() instanceof IOException) {
                logger.error("Disconnecting switch {} due to IO Error: {}",
                        e.getChannel().getId(), e.getCause().getMessage());
                ctx.getChannel().close();
                sw = (EnhancedSwitchHandler)channelIDToSwitchMap.get(e.getChannel().getId());
                sw.stop();
            /*
            } else if (e.getCause() instanceof SwitchStateException) {
                logger.error("Disconnecting switch {} due to switch state error: {}",
                        e.getChannel().getId(), e.getCause().getMessage());
                ctx.getChannel().close();
                channelIDToSwitchMap.remove(e.getChannel().getId());

            } else if (e.getCause() instanceof MessageParseException) {
                logger.error("Disconnecting switch {} due to message parse error Error : {}",
                        e.getChannel().getId(), e.getCause().getMessage());
                ctx.getChannel().close();
                sw = (EnhancedSwitchHandler)channelIDToSwitchMap.get(e.getChannel().getId());
                sw.stop(); */
            } else if (e.getCause() instanceof RejectedExecutionException) {
                logger.warn("Could not process message: queue full");
                ctx.getChannel().close();
                sw = (EnhancedSwitchHandler)channelIDToSwitchMap.get(e.getChannel().getId());
                sw.stop();
            } else {
                logger.error("Error while processing message from switch {} Error : {}",
                        e.getChannel().getId(), e.getCause().getMessage());
                e.getCause().printStackTrace();
                ctx.getChannel().close();
                sw = (EnhancedSwitchHandler)channelIDToSwitchMap.get(e.getChannel().getId());
                sw.stop();
            }

            statsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.EXCEPTION_CAUGHT);
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
                throws Exception {
            Integer messageChannelId = e.getChannel().getId();
            ISwitch swHan = (EnhancedSwitchHandler)channelIDToSwitchMap.get(messageChannelId);

            if (e.getMessage() instanceof List) {
                //@SuppressWarnings("unchecked")
                List<OFMessage> msglist = (List<OFMessage>)e.getMessage();
                if (msglist != null){ // this check actually brought down rate to some extent - weird !!!
                    for (OFMessage ofm : msglist) {
                        try {

                            // Do the actual packet processing
                            processOFMessage(ofm, messageChannelId);
                        }
                        catch (Exception ex) {
                            // We are the last handler in the stream, so run the
                            // exception through the channel again by passing in
                            // ctx.getChannel().
                            Channels.fireExceptionCaught(ctx.getChannel(), ex);
                        }
                    }
                }
            }

            // Flush all flow-mods/packet-out/stats generated from this "train"
            swHan.flushBufferedMessages();

            statsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.MESSAGE_RECEIVED);


        }


        public void processOFMessage(OFMessage ofm, Integer channelID){
            ISwitch switchHandler = channelIDToSwitchMap.get(channelID);
            statsHandler.countForEntitySimpleMeasurement(channelID, TrafficStatisticsHandler.ENTITY_COUNTER_RCV_MSG);
            if (switchHandler != null){
                switchHandler.handleMessage(ofm);
            }
        }


    }


    protected class ControllerServerBootstrap{

        private int workerThreads = 0;
        private EnhancedController controller = null;

        public ControllerServerBootstrap(EnhancedController controller){
            this.controller = controller;
        }


        public void startServer(int numWorkerThreads, String openFlowHost, int openFlowPort, OFChannelHandler ofchan){
            this.workerThreads = numWorkerThreads;
            try {
                final ServerBootstrap bootstrap = createServerBootStrap();

                 bootstrap.setOption("reuseAddr", true);
                 bootstrap.setOption("child.keepAlive", true);
                 bootstrap.setOption("child.tcpNoDelay", true);
                 bootstrap.setOption("child.receiveBufferSize", EnhancedController.RECEIVE_BUFFER_SIZE);
                 bootstrap.setOption("child.sendBufferSize", EnhancedController.SEND_BUFFER_SIZE);

                 // better to have an receive buffer predictor
                 //bootstrap.setOption("receiveBufferSizePredictorFactory",
                 //      new AdaptiveReceiveBufferSizePredictorFactory());
                 //if the server is sending 1000 messages per sec, optimum write buffer water marks will
                 //prevent unnecessary throttling, Check NioSocketChannelConfig doc
                 //bootstrap.setOption("writeBufferLowWaterMark", WRITE_BUFFER_LOW_WATERMARK);
                 //bootstrap.setOption("writeBufferHighWaterMark", WRITE_BUFFER_HIGH_WATERMARK);

                 // TODO: IMPORTANT: If the threadpool is supplied as null, ExecutionHandler would
                 // not be present in pipeline. If the load increases and ordering is required ,
                 // use OrderedMemoryAwareThreadPoolExecutor as argument instead of null

                 /*
                 execHandler = new OrderedMemoryAwareThreadPoolExecutor(
                                 OMATPE_CORE_POOL_SIZE,
                                 OMATPE_PER_CHANNEL_SIZE,
                                 OMATPE_POOL_WIDE_SIZE,
                                 OMATPE_THREAD_KEEP_ALIVE_IN_MILLISECONDS,
                                 TimeUnit.MILLISECONDS,
                                 new ObjectSizeEstimator() {

                                    @Override
                                    public int estimateSize(Object o) {
                                        return 30000;
                                    }
                                },
                                Executors.defaultThreadFactory());     */

                 execHandler = new OrderedMemoryAwareThreadPoolExecutor(
                         OMATPE_CORE_POOL_SIZE,
                         OMATPE_PER_CHANNEL_SIZE,
                         OMATPE_POOL_WIDE_SIZE,
                         OMATPE_THREAD_KEEP_ALIVE_IN_MILLISECONDS,
                         TimeUnit.MILLISECONDS);



                 ChannelPipelineFactory pfact =
                         new OpenflowPipelineFactory(controller, execHandler);
                 bootstrap.setPipelineFactory(pfact);
                 InetSocketAddress sa =
                         (openFlowHost == null)
                         ? new InetSocketAddress(openFlowPort)
                         : new InetSocketAddress(openFlowHost, openFlowPort);
                 final ChannelGroup cg = new DefaultChannelGroup();
                 cg.add(bootstrap.bind(sa));


             } catch (Exception e) {
                 throw new RuntimeException(e);
             }

        }

        private ServerBootstrap createServerBootStrap() {
            if (workerThreads == 0) {
                return new ServerBootstrap(
                        new NioServerSocketChannelFactory(
                                Executors.newCachedThreadPool(),
                                Executors.newCachedThreadPool()));
            } else {
                return new ServerBootstrap(
                        new NioServerSocketChannelFactory(
                                Executors.newCachedThreadPool(),
                                Executors.newCachedThreadPool(), workerThreads));
            }
        }



    }


    /**
     * Method called by SwitchHandler once the handshake state is completed
     *
     * @param sw
     */
    public void switchAdded(SwitchEvent switchEv, Integer switchChannelID){

        ISwitch sw = switchEv.getSwitch();
        Long switchId = sw.getId();

        connectedSwitches.put(switchId, sw);
        statsHandler.countForSimpleMeasurement(TrafficStatisticsHandler.CONNECTED_SWITCHES);

        logger.info("Switch with DPID : {} connected ", switchId);

        notifySwitchAdded(sw);
    }


    /**
     * Method called by SwitchHandler switch is disconnected
     *
     * @param sw
     */

    public void switchDeleted(SwitchEvent switchEv, Integer switchChannelID){
        ISwitch sw = switchEv.getSwitch();
        disconnectSwitch(sw, switchChannelID);
    }


    /**
     * Method called by SwitchHandler when it encounters any errors
     *
     *
     * @param sw
     */

    public void switchError(SwitchEvent switchEv, Integer switchChannelID){

    }


    public void switchMessage(SwitchEvent switchEv, Integer switchChannelID){
        long startTime = 0L;
        long endTime = 0L;


        OFMessage msg = switchEv.getMsg();
        ISwitch sw = switchEv.getSwitch();
        if (msg != null) {
            //try{
            //    lock.lock();
                IMessageListener listener = messageListeners
                        .get(msg.getType());
                if (listener != null) {
                    //logger.debug("delegating to msg-receiver");
                    //startTime = System.nanoTime();
                    listener.receive(sw, msg);
                    //endTime = System.nanoTime();
                    //this.statsHandler.reportPacketInProcessingTime(endTime - startTime);
                }
            //}
            //finally{
            //    lock.unlock();
            //}
        }
    }

    public void disconnectSwitch(ISwitch sw, Integer switchChannelID){
        Long sid = null;
        if (((EnhancedSwitchHandler) sw).isOperational()) {
            sid = sw.getId();

            this.connectedSwitches.remove(sid);
            this.channelIDToSwitchMap.remove(switchChannelID);
            notifySwitchDeleted(sw);
        }
        //((EnhancedSwitchHandler) sw).stop();
        logger.info("Switch with DPID {} disconnected", sid);
        sw = null;
    }


    private void notifySwitchAdded(ISwitch sw) {
        if (switchStateListener != null) {
            switchStateListener.switchAdded(sw);
        }
    }

    private void notifySwitchDeleted(ISwitch sw) {
        if (switchStateListener != null) {
            switchStateListener.switchDeleted(sw);
        }
    }

    @Override
    public InetAddress getControllerIdForSwitch(Long id) {
        //Added to enable Cluster handling.

        if(id==null)
        {
            logger.debug("id is null");
            return null;
        }

        ISwitch sw = (ISwitch) connectedSwitches.get(id);

        SocketAddress sockAddr = sw.getLocalAddress();

        if( sockAddr != null && InetSocketAddress.class.isAssignableFrom(sockAddr.getClass())) {
            InetAddress localAddress = ((InetSocketAddress) sockAddr).getAddress();
            this.logger.debug("socketchannel local address is " + localAddress);
            return localAddress;
        }

        return null;
    }

}
