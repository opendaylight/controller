/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 * Copyright (c) 2020 PANTHEON.tech, s.r.o
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.jmx.mbeans;

import java.util.List;

/**
 * MBean interface to raise the datastore related alarms.
 */
public interface DatastoreExceptionCountAlarmMBean {

    void setRaiseAlarmObject(List<String> raiseAlarmObject);

    List<String> getRaiseAlarmObject();

    void setClearAlarmObject(List<String> clearAlarmObject);

    List<String> getClearAlarmObject();

    void raiseAlarm(String alarmName, String additionalText, String source);

    void clearAlarm(String alarmName, String additionalText, String source);
}