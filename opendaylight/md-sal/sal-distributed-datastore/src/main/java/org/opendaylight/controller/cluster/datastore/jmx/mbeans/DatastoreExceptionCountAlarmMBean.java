/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.jmx.mbeans;

import java.util.ArrayList;

/**
 * MBean interface to raise the datastore related alarms.
 * @author evijayd
 *
 */
public interface DatastoreExceptionCountAlarmMBean {

    void setRaiseAlarmObject(ArrayList<String> raiseAlarmObject);

    ArrayList<String> getRaiseAlarmObject();

    void setClearAlarmObject(ArrayList<String> clearAlarmObject);

    ArrayList<String> getClearAlarmObject();

    void raiseAlarm(String alarmName, String additionalText, String source);

    void clearAlarm(String alarmName, String additionalText, String source);
}