/*
 * Copyright (c) 2018 Ericsson Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2020 PANTHEON.tech, s.r.o.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import akka.pattern.AskTimeoutException;
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
 * This is used to track the exception count that happened during the transaction. As of now, it is tailored to fit
 * AskTimeoutException, where in future other exception could be added. This is used in the DatastoreExceptionCountMBean
 * to provide the count to outside world.
 */
@SuppressWarnings({"checkstyle:IllegalCatch"})
@SuppressFBWarnings({"ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD","DM_DEFAULT_ENCODING","NP_NULL_ON_SOME_PATH_EXCEPTION",
        "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE","OS_OPEN_STREAM_EXCEPTION_PATH"})
public final class DatastoreExceptionTracker {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreExceptionTracker.class);

    /**
     * Pattern used to extract the ask timout value from the message string.
     */
    // FIXME: it is *us* who is specifying the timeout, hence we should have a place where we keep it
    private static final Pattern MSG_PATTERN_TIMEWAY = Pattern.compile(".*after \\[([\\d\\.]*).*");

    // FIXME: this should probably go through logging filtering
    private static final String HISTORY_FILE_NAME =
        System.getProperty("user.dir") + "/data/exception_count_tracker.txt";

    // FIXME: get rid of this global instance
    private static final DatastoreExceptionTracker INSTANCE = new DatastoreExceptionTracker();


    // FIXME: "5000" of what? why a String?
    private static final String DEFAULT_TIME_WAIT = "5000";

    private static final String SHARDMANAGER_CONFIG = "shardmanager-config";
    private static final String SHARD_INVENTORY = "shard-inventory";
    private static final String SHARD_TOPOLOGY = "shard-topology";
    private static final String SHARD_DEFAULT = "shard-default";
    private static final String SHARD_PMCOUNTERS = "shard-pmcounters";
    private static final String DEFAULT_SHARD = "eos";
    private static final String INVENTORY = "inventory";
    private static final String TOPOLOGY = "topology";
    private static final String PMCOUNTERS = "pmcounters";
    private static final String DEFAULT = "default";
    private static final String DEFAULT_SHARD_TYPE = "operational";
    private static final String CONFIG_SHARD = "config";

    private final ResetExceptionTracker resetExceptionTracker = new ResetExceptionTracker();
    private final Map<String, LongAdder> exceptionTraker = new ConcurrentHashMap<>();
    private final DatastoreExceptionTrackerAlarmAgent datastoreExceptionTrackerAlarmAgent;
    private final DatastoreExceptionCountMXBeanImpl datastoreExceptionCountMXBeanImpl;

    // FIXME: this looks like a configuration item
    private long resetTimerInterval = Long.getLong("datastore-exceptioncount-timer-interval", 600000);
    private long exceptionCountThreshold = Long.getLong("datastore-ate-threshold", 100);

    // FIXME: make this final
    private Timer timer = new Timer(true);

    private DatastoreExceptionTracker() {
        datastoreExceptionCountMXBeanImpl = new DatastoreExceptionCountMXBeanImpl();
        datastoreExceptionCountMXBeanImpl.registerMBean();
        datastoreExceptionTrackerAlarmAgent =
            new DatastoreExceptionTrackerAlarmAgent(new DatastoreExceptionCountAlarm());

        timer.scheduleAtFixedRate(resetExceptionTracker, 0, resetTimerInterval);
    }

    public static DatastoreExceptionTracker getInstance() {
        return INSTANCE;
    }

    public long getAskTimeoutExceptionCount() {
        long totalCount = 0;
        for (Map.Entry<String, LongAdder> entry : exceptionTraker.entrySet()) {
            totalCount = totalCount + entry.getValue().longValue();
        }
        return totalCount;
    }

    public void incrementAskTimeoutExceptionCounter(final AskTimeoutException cause, final String counterName) {
        final String counterKey = getExceptionTrackerCounterName(cause, counterName);
        exceptionTraker.computeIfAbsent(counterKey, key -> new LongAdder()).increment();
    }

    public String getDetailedATECounter() {
        return exceptionTraker.toString();
    }

    public long getResetTimerInterval() {
        return resetTimerInterval;
    }

    public void setResetTimerInterval(final long timerInterval) {
        LOG.debug("rescheduling the timer interval: {}" , resetTimerInterval);
        try {
            resetTimerInterval = timerInterval;

            // FIXME: OMDF: we really just want to reschedule the task!
            timer.cancel();
            timer = new Timer(true);

            timer.scheduleAtFixedRate(new ResetExceptionTracker(), 0, resetTimerInterval);
        } catch (Exception e) {
            LOG.debug("error while reseting timer", e);
        }
        LOG.debug("Time is rescheduled to the new timer interval: {}" , resetTimerInterval);
    }

    public long getExceptionCountThreshold() {
        return exceptionCountThreshold;
    }

    public void setExceptionCountThreshold(final long exceptionCountThreshold) {
        this.exceptionCountThreshold = exceptionCountThreshold;
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
    private static String getExceptionTrackerCounterName(final Throwable throwable, final String transactionClassName) {
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
        Matcher matcher = MSG_PATTERN_TIMEWAY.matcher(exceptionMessage);
        if (matcher.find()) {
            timewait = matcher.group(1);
        }
        StringBuilder stringBuilder = new StringBuilder().append("ate_").append(shardType).append("_").append(shard)
                .append("_").append(transactionClassName).append("_").append(timewait).append("_counter");
        LOG.debug("Ask Timeout Exception Tracker key: {}" , stringBuilder.toString());
        return stringBuilder.toString();
    }

    private class ResetExceptionTracker extends TimerTask {
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

        private boolean alarmRaised = false;

        private void raiseAlarm() {
            final long count = getAskTimeoutExceptionCount();
            LOG.debug("ATE exception count: {}", count);
            try {
                if (count > exceptionCountThreshold) {
                    LOG.debug("Raising the datastore exception count exceeds the threshold alarm:{}",
                             exceptionCountThreshold);
                    datastoreExceptionTrackerAlarmAgent.raiseDatastoreExceptionCountAlarm("DATASTORE");
                    LOG.debug("Datastore exception count exceeds the threshold alarm raised");
                    alarmRaised = true;
                } else if (count == 0) {
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

        // FIXME: clean this up
        private void createHistory() throws IOException {
            if (getAskTimeoutExceptionCount() == 0) {
                LOG.debug("No AskTimeoutException for this timer interval, hence not appending the history file");
                return;
            }
            FileWriter fw = null;
            try {
                fw = new FileWriter(HISTORY_FILE_NAME, true);
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
