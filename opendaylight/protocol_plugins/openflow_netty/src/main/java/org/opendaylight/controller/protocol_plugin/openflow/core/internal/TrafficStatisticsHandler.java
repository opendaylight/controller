package org.opendaylight.controller.protocol_plugin.openflow.core.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class TrafficStatisticsHandler {


    private static final Logger logger = LoggerFactory
            .getLogger(EnhancedController.class);

    private Timeout statsTaskHandle = null;

    private Map<String, AtomicLong> currentCounterMap = new ConcurrentHashMap<String, AtomicLong>();
    private Map<String, AtomicLong> lastCounterMap = new ConcurrentHashMap<String, AtomicLong>();
    private Map<String, Long> lastMeasurementTStamp = new ConcurrentHashMap<String, Long>();
    private List<String> rawRateMeasurementData = new ArrayList<String>();

    private ConcurrentHashMap<Integer, AtomicLong> msgRcvEntityCounter =
            new ConcurrentHashMap<Integer, AtomicLong>();
    private ConcurrentHashMap<Integer, AtomicLong> msgSndEntityCounter =
            new ConcurrentHashMap<Integer, AtomicLong>();



    private static final long STATISTICS_RATE_INTERVAL = 25000;
    private static final int STATISTICS_PRINT_INTREVAL = 180;
    private static List<Long> packetInProcessingTimeList = new ArrayList<Long>();
    private static List<Integer> pendingTaskCountList = new ArrayList<Integer>();


    public static final String ENTITY_COUNTER_RCV_MSG = "SWITCHWISE_RCV_MSG_COUNT";
    public static final String ENTITY_COUNTER_SND_MSG = "SWITCHWISE_SND_MSG_COUNT";

    private HashedWheelTimer hashedWheelTimer = null;

    public static final String ADDED_SWITCHES = "ADDED_SWITCHES";
    public static final String CONNECTED_SWITCHES = "CONNECTED_SWITCHES";
    public static final String DELETED_SWITCHES = "DELETED_SWITCHES";
    public static final String DISCONNECTED_SWITCHES = "DISCONNECTED_SWITCHES";
    public static final String SWITCH_ERROR = "SWITCH_ERROR";



    public static final String EXCEPTION_CAUGHT = "EXCEPTION_CAUGHT";
    public static final String MESSAGE_RECEIVED = "MESSAGE_RECEIVED";  // DO RATE-MEASUREMENTS

    public static final String MSG_LISTENER_INVOCATION = "MSG_LISTENER_INVOCATION";
    public static final String HELLO_RECEIVED = "HELLO_RECEIVED";
    public static final String HELLO_SENT = "HELLO_SENT";
    public static final String ECHO_REQUEST_SENT = "ECHO_REQUEST_SENT";
    public static final String ECHO_REQUEST_RECEIVED = "ECHO_REQUEST_RECEIVED";
    public static final String ECHO_REPLY_SENT = "ECHO_REPLY_SENT";
    public static final String ECHO_REPLY_RECEIVED = "ECHO_REPLY_RECEIVED";
    public static final String FEATURES_REQUEST_SENT = "FEATURES_REQUEST_SENT";
    public static final String FEATURES_REQUEST_RECEIVED = "FEATURES_REQUEST_RECEIVED";
    public static final String FEATURES_REPLY_SENT = "FEATURES_REPLY_SENT";
    public static final String FEATURES_REPLY_RECEIVED = "FEATURES_REPLY_RECEIVED";
    public static final String CONFIG_REQUEST_SENT = "CONFIG_REQUEST_SENT";
    public static final String CONFIG_REQUEST_RECEIVED = "CONFIG_REQUEST_RECEIVED";
    public static final String CONFIG_REPLY_SENT = "CONFIG_REPLY_SENT";
    public static final String CONFIG_REPLY_RECEIVED = "CONFIG_REPLY_RECEIVED";
    public static final String BARRIER_REQUEST_SENT = "BARRIER_REQUEST_SENT";
    public static final String BARRIER_REPLY_RECEIVED = "BARRIER_REPLY_RECEIVED";
    public static final String ERROR_MSG_RECEIVED = "ERROR_MSG_RECEIVED";
    public static final String PORT_STATUS_RECEIVED = "PORT_STATUS";
    public static final String PACKET_IN_RECEIVED = "PACKET_IN";        // DO RATE-MEASUREMENTS
    public static final String FLOW_MOD_SENT = "FLOW_MOD_SENT";            // DO RATE-MEASUREMENTS ==> To be determined as to where to collect this data from
    public static final String STATS_REQUEST_SENT = "STATS_REQUEST_SENT";     // DO RATE-MEASUREMENTS ==> To be determined as to where to collect this data from
    public static final String STATS_RESPONSE_RECEIVED = "STATS_RESPONSE_RECEIVED";

    public static final String UPDATE_PHYSICAL_PORT = "UPDATE_PHYSICAL_PORT";

    private static final int TASK_SCHEDULE_INITIAL_DELAY_IN_SECONDS = 10;

    private int trackPktInProcessing = 0;
    private static final int PKT_IN_PROCESSING_DURATION_SAMPLING_COUNT = 100000;


    public TrafficStatisticsHandler(HashedWheelTimer timer){
        this.hashedWheelTimer = timer;
    }


    public void init(){

        currentCounterMap.put(MSG_LISTENER_INVOCATION, new AtomicLong(0));
        currentCounterMap.put(ADDED_SWITCHES, new AtomicLong(0));
        currentCounterMap.put(DELETED_SWITCHES, new AtomicLong(0));
        currentCounterMap.put(CONNECTED_SWITCHES, new AtomicLong(0));
        currentCounterMap.put(DISCONNECTED_SWITCHES, new AtomicLong(0));
        currentCounterMap.put(SWITCH_ERROR, new AtomicLong(0));
        currentCounterMap.put(HELLO_RECEIVED, new AtomicLong(0));
        currentCounterMap.put(HELLO_SENT, new AtomicLong(0));
        currentCounterMap.put(ECHO_REQUEST_SENT, new AtomicLong(0));
        currentCounterMap.put(ECHO_REQUEST_RECEIVED, new AtomicLong(0));
        currentCounterMap.put(ECHO_REPLY_SENT, new AtomicLong(0));
        currentCounterMap.put(ECHO_REPLY_RECEIVED, new AtomicLong(0));
        currentCounterMap.put(EXCEPTION_CAUGHT, new AtomicLong(0));
        currentCounterMap.put(MESSAGE_RECEIVED, new AtomicLong(0));
        currentCounterMap.put(FEATURES_REQUEST_SENT, new AtomicLong(0));
        currentCounterMap.put(FEATURES_REQUEST_RECEIVED, new AtomicLong(0));
        currentCounterMap.put(FEATURES_REPLY_SENT, new AtomicLong(0));
        currentCounterMap.put(FEATURES_REPLY_RECEIVED, new AtomicLong(0));
        currentCounterMap.put(CONFIG_REQUEST_SENT, new AtomicLong(0));
        currentCounterMap.put(CONFIG_REQUEST_RECEIVED, new AtomicLong(0));
        currentCounterMap.put(CONFIG_REPLY_SENT, new AtomicLong(0));
        currentCounterMap.put(CONFIG_REPLY_RECEIVED, new AtomicLong(0));
        currentCounterMap.put(BARRIER_REQUEST_SENT, new AtomicLong(0));
        currentCounterMap.put(BARRIER_REPLY_RECEIVED, new AtomicLong(0));
        currentCounterMap.put(ERROR_MSG_RECEIVED, new AtomicLong(0));
        currentCounterMap.put(PORT_STATUS_RECEIVED, new AtomicLong(0));
        currentCounterMap.put(PACKET_IN_RECEIVED, new AtomicLong(0));
        currentCounterMap.put(FLOW_MOD_SENT, new AtomicLong(0));
        currentCounterMap.put(STATS_REQUEST_SENT, new AtomicLong(0));
        currentCounterMap.put(STATS_RESPONSE_RECEIVED, new AtomicLong(0));
        currentCounterMap.put(UPDATE_PHYSICAL_PORT, new AtomicLong(0));

        lastCounterMap.put(MSG_LISTENER_INVOCATION, new AtomicLong(0));
        lastCounterMap.put(ADDED_SWITCHES, new AtomicLong(0));
        lastCounterMap.put(DELETED_SWITCHES, new AtomicLong(0));
        lastCounterMap.put(CONNECTED_SWITCHES, new AtomicLong(0));
        lastCounterMap.put(DISCONNECTED_SWITCHES, new AtomicLong(0));
        lastCounterMap.put(SWITCH_ERROR, new AtomicLong(0));
        lastCounterMap.put(HELLO_RECEIVED, new AtomicLong(0));
        lastCounterMap.put(HELLO_SENT, new AtomicLong(0));
        lastCounterMap.put(FEATURES_REQUEST_SENT, new AtomicLong(0));
        lastCounterMap.put(FEATURES_REQUEST_RECEIVED, new AtomicLong(0));
        lastCounterMap.put(ECHO_REQUEST_SENT, new AtomicLong(0));
        lastCounterMap.put(ECHO_REQUEST_RECEIVED, new AtomicLong(0));
        lastCounterMap.put(ECHO_REPLY_SENT, new AtomicLong(0));
        lastCounterMap.put(ECHO_REPLY_RECEIVED, new AtomicLong(0));
        lastCounterMap.put(EXCEPTION_CAUGHT, new AtomicLong(0));
        lastCounterMap.put(MESSAGE_RECEIVED, new AtomicLong(0));
        lastCounterMap.put(FEATURES_REPLY_SENT, new AtomicLong(0));
        lastCounterMap.put(FEATURES_REPLY_RECEIVED, new AtomicLong(0));
        lastCounterMap.put(CONFIG_REQUEST_SENT, new AtomicLong(0));
        lastCounterMap.put(CONFIG_REQUEST_RECEIVED, new AtomicLong(0));
        lastCounterMap.put(CONFIG_REPLY_SENT, new AtomicLong(0));
        lastCounterMap.put(CONFIG_REPLY_RECEIVED, new AtomicLong(0));
        lastCounterMap.put(BARRIER_REQUEST_SENT, new AtomicLong(0));
        lastCounterMap.put(BARRIER_REPLY_RECEIVED, new AtomicLong(0));
        lastCounterMap.put(ERROR_MSG_RECEIVED, new AtomicLong(0));
        lastCounterMap.put(PORT_STATUS_RECEIVED, new AtomicLong(0));
        lastCounterMap.put(PACKET_IN_RECEIVED, new AtomicLong(0));
        lastCounterMap.put(FLOW_MOD_SENT, new AtomicLong(0));
        lastCounterMap.put(STATS_REQUEST_SENT, new AtomicLong(0));
        lastCounterMap.put(STATS_RESPONSE_RECEIVED, new AtomicLong(0));
        lastCounterMap.put(UPDATE_PHYSICAL_PORT, new AtomicLong(0));

        lastMeasurementTStamp.put(MSG_LISTENER_INVOCATION, new Long(0));
        lastMeasurementTStamp.put(ADDED_SWITCHES, new Long(0));
        lastMeasurementTStamp.put(DELETED_SWITCHES, new Long(0));
        lastMeasurementTStamp.put(CONNECTED_SWITCHES, new Long(0));
        lastMeasurementTStamp.put(DISCONNECTED_SWITCHES, new Long(0));
        lastMeasurementTStamp.put(SWITCH_ERROR, new Long(0));
        lastMeasurementTStamp.put(HELLO_RECEIVED, new Long(0));
        lastMeasurementTStamp.put(HELLO_SENT, new Long(0));
        lastMeasurementTStamp.put(ECHO_REQUEST_SENT, new Long(0));
        lastMeasurementTStamp.put(ECHO_REQUEST_RECEIVED, new Long(0));
        lastMeasurementTStamp.put(ECHO_REPLY_SENT, new Long(0));
        lastMeasurementTStamp.put(ECHO_REPLY_RECEIVED, new Long(0));
        lastMeasurementTStamp.put(EXCEPTION_CAUGHT, new Long(0));
        lastMeasurementTStamp.put(MESSAGE_RECEIVED, new Long(0));
        lastMeasurementTStamp.put(FEATURES_REQUEST_SENT, new Long(0));
        lastMeasurementTStamp.put(FEATURES_REQUEST_RECEIVED, new Long(0));
        lastMeasurementTStamp.put(FEATURES_REPLY_SENT, new Long(0));
        lastMeasurementTStamp.put(FEATURES_REPLY_RECEIVED, new Long(0));
        lastMeasurementTStamp.put(CONFIG_REQUEST_SENT, new Long(0));
        lastMeasurementTStamp.put(CONFIG_REQUEST_RECEIVED, new Long(0));
        lastMeasurementTStamp.put(CONFIG_REPLY_SENT, new Long(0));
        lastMeasurementTStamp.put(CONFIG_REPLY_RECEIVED, new Long(0));
        lastMeasurementTStamp.put(BARRIER_REQUEST_SENT, new Long(0));
        lastMeasurementTStamp.put(BARRIER_REPLY_RECEIVED, new Long(0));
        lastMeasurementTStamp.put(ERROR_MSG_RECEIVED, new Long(0));
        lastMeasurementTStamp.put(PORT_STATUS_RECEIVED, new Long(0));
        lastMeasurementTStamp.put(PACKET_IN_RECEIVED, new Long(0));
        lastMeasurementTStamp.put(FLOW_MOD_SENT, new Long(0));
        lastMeasurementTStamp.put(STATS_REQUEST_SENT, new Long(0));
        lastMeasurementTStamp.put(STATS_RESPONSE_RECEIVED, new Long(0));
        lastMeasurementTStamp.put(UPDATE_PHYSICAL_PORT, new Long(0));

        /*
        rateMap.put(HELLO_SENT, new Double(0.00000000));
        rateMap.put(FEATURES_REQUEST, new Double(0.00000000));
        rateMap.put(FEATURES_REPLY, new Double(0.00000000));
        rateMap.put(CONFIG_REQUEST, new Double(0.00000000));
        rateMap.put(CONFIG_REPLY, new Double(0.00000000));
        rateMap.put(PORT_STATUS, new Double(0.00000000));
        rateMap.put(PACKET_IN, new Double(0.00000000));
        rateMap.put(FLOW_MOD_SENT, new Double(0.00000000));


        history.put(HELLO_SENT, new ArrayList());
        history.put(FEATURES_REQUEST, new ArrayList());
        history.put(FEATURES_REPLY, new ArrayList());
        history.put(CONFIG_REQUEST, new ArrayList());
        history.put(CONFIG_REPLY, new ArrayList());
        history.put(PORT_STATUS, new ArrayList());
        history.put(PACKET_IN, new ArrayList());
        history.put(FLOW_MOD_SENT, new ArrayList());
        */

        statsTaskHandle = this.hashedWheelTimer.newTimeout(new StatsOutTask(),
                TASK_SCHEDULE_INITIAL_DELAY_IN_SECONDS, TimeUnit.SECONDS);

    }

    public void stopStatsHandler(){
        if (statsTaskHandle != null){
            statsTaskHandle.cancel();
        }
    }

    public void reportPacketInProcessingTime(long duration){
        trackPktInProcessing++;
        if (trackPktInProcessing > PKT_IN_PROCESSING_DURATION_SAMPLING_COUNT){
            packetInProcessingTimeList.add(new Long(duration));
            trackPktInProcessing = 0;
        }
    }

    public void addEntityForCounter(Integer entityID, String counterType){
        if (counterType.equalsIgnoreCase(ENTITY_COUNTER_RCV_MSG)){
            msgRcvEntityCounter.put(entityID, new AtomicLong(0));
        }
        else{
            msgSndEntityCounter.put(entityID, new AtomicLong(0));
        }
    }

    public void countForEntitySimpleMeasurement(Integer entityID, String counterType){
        if (counterType.equalsIgnoreCase(ENTITY_COUNTER_RCV_MSG)){
            //msgRcvEntityCounter.get(entityID).incrementAndGet();
        }
        else{
            //msgSndEntityCounter.get(entityID).incrementAndGet();
        }
    }


    public void countForSimpleMeasurement(String counterName){
        currentCounterMap.get(counterName).incrementAndGet();
    }

    public void countForRateMeasurement(String counterName){

        long currCntr = currentCounterMap.get(counterName).incrementAndGet();
        if (lastMeasurementTStamp.get(counterName) == 0){
            lastMeasurementTStamp.put(counterName, System.currentTimeMillis());
        }

        Long currentCount = new Long(currCntr);
        Long lastCount = lastCounterMap.get(counterName).get();

        //Double rate = 0.00000000000;
        if ((currentCount - lastCount) == STATISTICS_RATE_INTERVAL){
            Long currentTime = System.currentTimeMillis();
            Long lastTime = lastMeasurementTStamp.get(counterName);
            //rate = new Double((STATISTICS_RATE_INTERVAL/(currentTime-lastTime))*1000); //convert to count/sec
            rawRateMeasurementData.add("CN:" + counterName +
                    ",CC:" + currentCount +
                    ",LC:" + lastCount +
                    ",CT:" + currentTime +
                    ",LT:" + lastTime +
                    ",CV:" + ((STATISTICS_RATE_INTERVAL/(currentTime-lastTime))*1000));
            lastCounterMap.put(counterName, new AtomicLong(currentCount));
            lastMeasurementTStamp.put(counterName, currentTime);
            //history.get(counterName).add(String.valueOf(rate.doubleValue()));
            //rateMap.put(counterName, rate);
        }

    }


    private class StatsOutTask implements TimerTask {

        @Override
        public void run(Timeout timeout) throws Exception {

            statsTaskHandle = timeout;
            logger.warn(">>>>>>Raw Counter values at controller BEGIN<<<<<<<<");

            for (Entry<String, AtomicLong> entry : currentCounterMap.entrySet()){
                logger.warn("{} {}", entry.getKey(), entry.getValue());
            }
            logger.warn(">>>>>>Counter values at controller END  <<<<<<<<");

            logger.warn(">>>>>>Entity Counter values at controller BEGIN<<<<<<<<");

            for (Entry<Integer, AtomicLong> entry : msgRcvEntityCounter.entrySet()){
                logger.warn("SwitchID {} : Rcv Msg Count {}", entry.getKey(), entry.getValue());
            }
            logger.warn(">>>>>>Entity Counter values at controller END  <<<<<<<<");

            logger.warn(">>>>>>Raw data rate values at controller BEGIN<<<<<<<<");

            for (String str : rawRateMeasurementData ){
                logger.warn("{}", str);
            }
            logger.warn(">>>>>>Raw data rate values at controller END  <<<<<<<<");


            if (packetInProcessingTimeList.size() > 0){
                logger.warn("================ MAX PACKET_IN PROC TIME in microseconds : {}",
                        Collections.max(packetInProcessingTimeList)/1000);
                logger.warn("================ MIN PACKET_IN PROC TIME in microseconds : {}",
                        Collections.min(packetInProcessingTimeList)/1000);
                long v = 0L;
                int track = 0;
                for (Long val : packetInProcessingTimeList){
                    v = v + val.longValue();
                    track++;
                }
                logger.warn("================ AVG PACKET_IN PROC TIME in microseconds : {}",
                        ((double)(v/track))/1000);
            }
            hashedWheelTimer.newTimeout(this, STATISTICS_PRINT_INTREVAL, TimeUnit.SECONDS);
        }
    }

}
