/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 * Copyright (c) 2020 PANTHEON.tech, s.r.o
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.jmx.mbeans;

import java.util.ArrayList;
import java.util.List;
import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the MBean to raise and clear the datastore related alarms.
 */
public class DatastoreExceptionCountAlarm extends NotificationBroadcasterSupport
        implements DatastoreExceptionCountAlarmMBean {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreExceptionCountAlarm.class);

    // FIXME: clean up livecycle of these!
    private List<String> raiseAlarmObject = new ArrayList<>();
    private List<String> clearAlarmObject = new ArrayList<>();
    private long sequenceNumber = 1;

    @Override
    public void setRaiseAlarmObject(final List<String> raiseAlarmObject) {
        this.raiseAlarmObject = raiseAlarmObject;
        Notification notification = new AttributeChangeNotification(this, sequenceNumber++, System.currentTimeMillis(),
                "raise alarm object notified ", "raiseAlarmObject", "ArrayList", "", this.raiseAlarmObject);
        sendNotification(notification);
    }

    @Override
    public List<String> getRaiseAlarmObject() {
        return raiseAlarmObject;
    }

    @Override
    public void setClearAlarmObject(final List<String> clearAlarmObject) {
        this.clearAlarmObject = clearAlarmObject;
        Notification notification = new AttributeChangeNotification(this, sequenceNumber++, System.currentTimeMillis(),
                "clear alarm object notified ", "clearAlarmObject", "ArrayList", "", this.clearAlarmObject);
        sendNotification(notification);
    }

    @Override
    public List<String> getClearAlarmObject() {
        return clearAlarmObject;
    }

    @Override
    public void raiseAlarm(final String alarmName, final String additionalText, final String serviceName) {
        LOG.debug("Exception count reached the threshold value, raising alarm");
        raiseAlarmObject.add(alarmName);
        raiseAlarmObject.add(additionalText);
        raiseAlarmObject.add(serviceName);
        setRaiseAlarmObject(raiseAlarmObject);
        raiseAlarmObject.clear();
    }

    @Override
    public void clearAlarm(final String alarmName, final String additionalText, final String serviceName) {
        LOG.debug("Exception count reduced below the threshold value, clearing the alarm");
        clearAlarmObject.add(alarmName);
        clearAlarmObject.add(additionalText);
        clearAlarmObject.add(serviceName);
        setClearAlarmObject(clearAlarmObject);
        clearAlarmObject.clear();
    }
}