/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
 * This class is responsible to register the alarm MBean, so that the framework
 * will look into its notification and raise the actual alarm.
 *
 * @author evijayd
 *
 */
@SuppressWarnings({"checkstyle:IllegalCatch"})
@SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
public final class DatastoreExceptionTrackerAlarmAgent {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreExceptionTrackerAlarmAgent.class);
    private static MBeanServer mbs = null;
    private static ObjectName alarmObjectName = null;
    private static final String BEAN_NAME = "SDNC.FM:name=DatastoreExceptionCountAlarmMBean";
    private DatastoreExceptionCountAlarm datastoreExceptionCountAlarm;
    private static final String ALARM_NAME = "DatastoreExceptionCountAlarm";


    public DatastoreExceptionTrackerAlarmAgent(DatastoreExceptionCountAlarm datastoreExceptionCountAlarmMXBeanImpl) {
        this.datastoreExceptionCountAlarm = datastoreExceptionCountAlarmMXBeanImpl;
        mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            alarmObjectName = new ObjectName(BEAN_NAME);
            LOG.debug("DatastoreExceptionCountAlarmMXBean ObjectName instance created");
        } catch (MalformedObjectNameException e) {
            LOG.error("ObjectName instance creation failed for Bean {} : ", BEAN_NAME, e);
        }
        registerAlarmMbean();
    }

    public void registerAlarmMbean() {
        try {
            if (!mbs.isRegistered(alarmObjectName)) {
                mbs.registerMBean(datastoreExceptionCountAlarm, alarmObjectName);
                LOG.info("Registered Mbean {} successfully", alarmObjectName);
            }
        } catch (Exception e) {
            LOG.error("Registeration failed for Mbean {} : ", alarmObjectName, e);
        }
    }

    public void raiseDatastoreExceptionCountAlarm(String serviceName) {
        String alarmText = getAdditionalText();
        LOG.debug("Raising ServiceFailure alarm with alarmText {} for service : {} ", alarmText, serviceName);
        try {
            mbs.invoke(alarmObjectName, "raiseAlarm", new Object[]{ALARM_NAME, alarmText, serviceName},
                    new String[]{String.class.getName(), String.class.getName(), String.class.getName()});
            LOG.debug("Invoked raiseAlarm function for Mbean {} for service {}", BEAN_NAME, serviceName);
        } catch (Exception e) {
            LOG.error("Invoking raiseAlarm method failed for Mbean {} : ", BEAN_NAME, e);
        }
    }

    public void clearServiceFailureAlarm(String serviceName) {
        String alarmText = getAdditionalText();
        LOG.debug("Clearing ServiceFailure alarm with alarmText {} for service : {} ", alarmText, serviceName);
        try {
            mbs.invoke(alarmObjectName, "clearAlarm", new Object[]{ALARM_NAME, alarmText, serviceName},
                    new String[]{String.class.getName(), String.class.getName(), String.class.getName()});
            LOG.debug("Invoked clearAlarm function for Mbean {} for service {}",BEAN_NAME,serviceName);
        } catch (Exception e) {
            LOG.error("Invoking clearAlarm method failed for Mbean {} : ", BEAN_NAME, e);
        }
    }

    private String getAdditionalText() {
        return "Datastore exception count has reached the threshold";
    }
}
