/*
 * Copyright (c) 2018 Ericsson Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.databroker;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.DatastoreExceptionCountAlarm;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.DatastoreExceptionCountMXBeanImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is used to track the exception count that happened during the
 * transaction. As of now, it is tailored to fit AskTimeoutException, where in
 * future other exception could be added. This is used in the
 * DatastoreExceptionCountMBean to provide the count to outside world.
 * @author evijayd
 */
@SuppressWarnings({"checkstyle:IllegalCatch"})
@SuppressFBWarnings({"ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD","DM_DEFAULT_ENCODING","NP_NULL_ON_SOME_PATH_EXCEPTION",
        "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE","OS_OPEN_STREAM_EXCEPTION_PATH"})
public final class DatastoreExceptionTracker {

    private static final Logger LOG = LoggerFactory.getLogger(DatastoreExceptionTracker.class);

    private static Map<String, LongAdder> exceptionTraker = new ConcurrentHashMap<String, LongAdder>();
    private static long resetTimerInterval = Long.getLong("datastore-exceptioncount-timer-interval", 600000);
    private static long exceptionCountThreshold = Long.getLong("datastore-ate-threshold", 100);
    private static DatastoreExceptionTrackerAlarmAgent datastoreExceptionTrackerAlarmAgent = null;
    private static ResetExceptionTracker resetExceptionTracker = new ResetExceptionTracker();
    private static DatastoreExceptionTracker datastoreExceptionTracker = new DatastoreExceptionTracker();
    private DatastoreExceptionCountMXBeanImpl datastoreExceptionCountMXBeanImpl = null;
    private static StringBuilder exceptionTrackerHistoryFile = new StringBuilder(System.getProperty("user.dir"))
            .append("/data/exception_count_tracker.txt");
    private static Timer timer = new Timer(true);

    private static String SHARDMANAGER_CONFIG = "shardmanager-config";
    private static String SHARD_INVENTORY = "shard-inventory";
    private static String SHARD_TOPOLOGY = "shard-topology";
    private static String SHARD_DEFAULT = "shard-default";
    private static String SHARD_PMCOUNTERS = "shard-pmcounters";
    private static String DEFAULT_SHARD = "eos";
    private static String INVENTORY = "inventory";
    private static String TOPOLOGY = "topology";
    private static String PMCOUNTERS = "pmcounters";
    private static String DEFAULT = "default";
    private static String DEFAULT_SHARD_TYPE = "operational";
    private static String CONFIG_SHARD = "config";
    private static String DEFAULT_TIME_WAIT = "5000";


    private DatastoreExceptionTracker() {
        datastoreExceptionCountMXBeanImpl = new DatastoreExceptionCountMXBeanImpl();
        datastoreExceptionCountMXBeanImpl.registerMBean();
        initializeAlarmAgent();
        initializeResetTimer();
    }

    public static DatastoreExceptionTracker getInstance() {
        return datastoreExceptionTracker;
    }

    public long getAskTimeoutExceptionCount() {
        long totalCount = 0;
        for (Map.Entry<String, LongAdder> entry : exceptionTraker.entrySet()) {
            totalCount = totalCount + entry.getValue().longValue();
        }
        return totalCount;
    }

    public void incrementAskTimeoutExceptionCounter(String exceptionCounterKey) {
        if (exceptionTraker.get(exceptionCounterKey) == null) {
            exceptionTraker.put(exceptionCounterKey, new LongAdder());
        }
        exceptionTraker.get(exceptionCounterKey).increment();
    }

    public String getDetailedATECounter() {
        return exceptionTraker.toString();
    }

    public long getResetTimerInterval() {
        return resetTimerInterval;
    }

    public void setResetTimerInterval(long timerInterval) {
        LOG.debug("rescheduling the timer interval: {}" , resetTimerInterval);
        try {
            resetTimerInterval = timerInterval;
            cancelTimer();
            timer = new Timer(true);
            timer.scheduleAtFixedRate(new ResetExceptionTracker(), 0, resetTimerInterval);
        } catch (Exception e) {
            LOG.debug("error while reseting timer", e);
        }
        LOG.debug("Time is rescheduled to the new timer interval: {}" , resetTimerInterval);
    }

    public static long getExceptionCountThreshold() {
        return exceptionCountThreshold;
    }

    public static void setExceptionCountThreshold(long exceptionCountThreshold) {
        DatastoreExceptionTracker.exceptionCountThreshold = exceptionCountThreshold;
    }

    public void initializeResetTimer() {
        timer = new Timer(true);
        timer.scheduleAtFixedRate(resetExceptionTracker, 0, resetTimerInterval);
    }

    private void initializeAlarmAgent() {
        DatastoreExceptionCountAlarm datastoreExceptionCountAlarmMXBeanImpl = new DatastoreExceptionCountAlarm();
        datastoreExceptionTrackerAlarmAgent = new DatastoreExceptionTrackerAlarmAgent(
        datastoreExceptionCountAlarmMXBeanImpl);
    }

    public static void cancelTimer() {
        timer.cancel();
    }

    /**
     * Utility method to get the key name for the 'exceptionTraker' hashmap. The
     * counter value is stored against this key. The key name is framed using
     * the throwable and the transaction class name.
     * @param throwable
     *          Throwable - from the classes where the asktimeout exception is thrown.
     * @param transactionClassName
     *            - the name of the class that is handling the exception.
     * @return A string - that is the key to retrieve the counter from the
     *         hashmap.
     */
    public String getExceptionTrackerCounterName(Throwable throwable, String transactionClassName) {
        String exceptionMessage = throwable.getMessage();
        String shardType = DEFAULT_SHARD_TYPE;
        String shard = DEFAULT_SHARD;
        String timewait = DEFAULT_TIME_WAIT;
        if (exceptionMessage.contains(SHARDMANAGER_CONFIG)) {
            shardType = CONFIG_SHARD;
        }
        if (exceptionMessage.contains(SHARD_INVENTORY)) {
            shard = INVENTORY;
        } else if (exceptionMessage.contains(SHARD_TOPOLOGY)) {
            shard = TOPOLOGY;
        } else if (exceptionMessage.contains(SHARD_DEFAULT)) {
            shard = DEFAULT;
        } else if (exceptionMessage.contains(SHARD_PMCOUNTERS)) {
            shard = PMCOUNTERS;
        }
        Pattern pattern = Pattern.compile(".*after \\[([\\d\\.]*).*");
        Matcher matcher = pattern.matcher(exceptionMessage);
        if (matcher.find()) {
            timewait = matcher.group(1);
        }
        StringBuilder stringBuilder = new StringBuilder().append("ate_").append(shardType).append("_").append(shard)
                .append("_").append(transactionClassName).append("_").append(timewait).append("_counter");
        LOG.debug("Ask Timeout Exception Tracker key: {}" , stringBuilder.toString());
        return stringBuilder.toString();
    }

    static class ResetExceptionTracker extends TimerTask {

        @Override
        public void run() {
            try {
                LOG.debug("running the ATE timer task");
                createHistory();
                raiseAlarm();
                resetCounters();
                LOG.debug("completed the ATE timer task");
            } catch (IOException e) {
                LOG.error("Error while running the ATE Reset Timer", e);
            }
        }

        private static boolean alarmRaised = false;

        private void raiseAlarm() {
            LOG.debug("ATE exception count: {}", datastoreExceptionTracker.getAskTimeoutExceptionCount());
            try {
                if (datastoreExceptionTracker.getAskTimeoutExceptionCount() > exceptionCountThreshold) {
                    LOG.debug("Raising the datastore exception count exceeds the threshold alarm:{}",
                             exceptionCountThreshold);
                    datastoreExceptionTrackerAlarmAgent.raiseDatastoreExceptionCountAlarm("DATASTORE");
                    LOG.debug("Datastore exception count exceeds the threshold alarm raised");
                    alarmRaised = true;
                } else if (datastoreExceptionTracker.getAskTimeoutExceptionCount() == 0) {
                    if (alarmRaised) {
                        LOG.debug("Raising the clear alarm for datastore exception count alarm");
                        datastoreExceptionTrackerAlarmAgent.clearServiceFailureAlarm("DATASTORE");
                        alarmRaised = false;
                    }
                }
            } catch (Exception e) {
                LOG.debug("Error while raising datastore alarm:", e);
            }
        }

        private void createHistory() throws IOException {
            if (datastoreExceptionTracker.getAskTimeoutExceptionCount() == 0) {
                LOG.debug("No AskTimeoutException for this timer interval, hence not appending the history file");
                return;
            }
            FileWriter fw = null;
            try {
                fw = new FileWriter(String.valueOf(exceptionTrackerHistoryFile), true);
            } catch (IOException e) {
                LOG.error("Error while opening the ATE counters history file", e);
            }
            Date date = new Date();
            fw.append(date.toString());
            fw.append("\n");
            fw.append("ATE Threshold:").append(String.valueOf(exceptionCountThreshold));
            fw.append("\n");
            for (Map.Entry<String, LongAdder> entry : exceptionTraker.entrySet()) {
                String key = entry.getKey();
                long value = entry.getValue().longValue();
                fw.append(key + " => " + value);
                fw.append("\n");
            }
            fw.append("\n");
            fw.close();
            LOG.debug("done creating the history file for the ask timeout exception tracker");
        }

        private void resetCounters() {
            try {
                for (Map.Entry<String, LongAdder> entry : exceptionTraker.entrySet()) {
                    exceptionTraker.put(entry.getKey(), new LongAdder());
                }
                LOG.debug("Reseting the ask timeout exception tracker counters");
            } catch (Exception e) {
                LOG.error("Error while reseting the ATE counters", e);
            }
        }
    }

}
