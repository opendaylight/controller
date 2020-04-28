/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 * Copyright (c) 2020 PANTHEON.tech, s.r.o
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.DatastoreExceptionCountAlarm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible to register the alarm MBean, so that the framework will look into its notification and
 * raise the actual alarm.
 */
@SuppressWarnings({"checkstyle:IllegalCatch"})
@SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
public final class DatastoreExceptionTrackerAlarmAgent {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreExceptionTrackerAlarmAgent.class);
    private static final String ALARM_NAME = "DatastoreExceptionCountAlarm";
    private static final String ADDITIONAL_TEXT = "Datastore exception count has reached the threshold";
    private static final MBeanServer MBS = ManagementFactory.getPlatformMBeanServer();
    private static final ObjectName ALARM_OBJECT_NAME;

    static {
        try {
            // FIXME: OMFG: are we really making an upcall to SDNC?!
            ALARM_OBJECT_NAME = new ObjectName("SDNC.FM:name=DatastoreExceptionCountAlarmMBean");
        } catch (MalformedObjectNameException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final DatastoreExceptionCountAlarm datastoreExceptionCountAlarm;

    public DatastoreExceptionTrackerAlarmAgent(final DatastoreExceptionCountAlarm alarm) {
        this.datastoreExceptionCountAlarm = alarm;
        try {
            if (!MBS.isRegistered(ALARM_OBJECT_NAME)) {
                MBS.registerMBean(datastoreExceptionCountAlarm, ALARM_OBJECT_NAME);
                LOG.info("Registered Mbean {} successfully", ALARM_OBJECT_NAME);
            }
        } catch (Exception e) {
            LOG.error("Registeration failed for Mbean {} : ", ALARM_OBJECT_NAME, e);
        }
    }

    public void raiseDatastoreExceptionCountAlarm(final String serviceName) {
        String alarmText = ADDITIONAL_TEXT;
        LOG.debug("Raising ServiceFailure alarm with alarmText {} for service : {} ", alarmText, serviceName);
        try {
            // FIXME: oh this is just too beautiful
            MBS.invoke(ALARM_OBJECT_NAME, "raiseAlarm", new Object[]{ALARM_NAME, alarmText, serviceName},
                    new String[]{String.class.getName(), String.class.getName(), String.class.getName()});
            LOG.debug("Invoked raiseAlarm function for Mbean {} for service {}", BEAN_NAME, serviceName);
        } catch (Exception e) {
            LOG.error("Invoking raiseAlarm method failed for Mbean {} : ", ALARM_OBJECT_NAME, e);
        }
    }

    public void clearServiceFailureAlarm(final String serviceName) {
        String alarmText = ADDITIONAL_TEXT;
        LOG.debug("Clearing ServiceFailure alarm with alarmText {} for service : {} ", alarmText, serviceName);
        try {
            // FIXME: oh this is just too beautiful
            MBS.invoke(ALARM_OBJECT_NAME, "clearAlarm", new Object[]{ALARM_NAME, alarmText, serviceName},
                    new String[]{String.class.getName(), String.class.getName(), String.class.getName()});
            LOG.debug("Invoked clearAlarm function for Mbean {} for service {}", ALARM_OBJECT_NAME, serviceName);
        } catch (Exception e) {
            LOG.error("Invoking clearAlarm method failed for Mbean {} : ", ALARM_OBJECT_NAME, e);
        }
    }
}
