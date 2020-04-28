/*
 * Copyright (c) 2018 Ericsson Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.jmx.mbeans;

/**
 * This is the MBean interface created to get the exception count during the transactions.
 */
public interface DatastoreExceptionCountMXBean {

    long getAskTimeoutExceptionCount();

    String getDetailedAskTimeoutExceptionCount();

    long getResetCounterTimerInterval();

    void setResetCounterTimerInterval(long resetTimerInterval);

    long getExceptionCountThreshold();

    void setExceptionCountThreshold(long exceptionCountThreshold);
}
