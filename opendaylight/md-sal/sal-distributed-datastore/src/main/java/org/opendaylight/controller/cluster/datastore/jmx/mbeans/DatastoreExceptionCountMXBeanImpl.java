/*
 * Copyright (c) 2018 Ericsson Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.jmx.mbeans;

import org.opendaylight.controller.cluster.databroker.DatastoreExceptionTracker;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the implementation for the DatastoreExceptionMXBean interface. It uses the DatastoreExceptionTracker to get
 * the counter values. It also has a setter to configure the timer interval in which the counter value should be reset.
 */
public class DatastoreExceptionCountMXBeanImpl extends AbstractMXBean implements DatastoreExceptionCountMXBean {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreExceptionCountMXBeanImpl.class);

    // FIXME: this should clearly be bound
    public DatastoreExceptionCountMXBeanImpl() {
        super("DatastoreExceptionCountMXBeanImpl", "DatastoreExceptionCountMXBean", "DatastoreExceptionCountMXBean");
    }

    @Override
    public long getAskTimeoutExceptionCount() {
        return DatastoreExceptionTracker.getInstance().getAskTimeoutExceptionCount();
    }

    @Override
    public String getDetailedAskTimeoutExceptionCount() {
        return DatastoreExceptionTracker.getInstance().getDetailedATECounter();
    }

    @Override
    public long getResetCounterTimerInterval() {
        return DatastoreExceptionTracker.getInstance().getResetTimerInterval();
    }

    @Override
    public void setResetCounterTimerInterval(final long resetTimerInterval) {
        LOG.debug("setting the timer interval to: {}", resetTimerInterval);
        DatastoreExceptionTracker.getInstance().setResetTimerInterval(resetTimerInterval);
    }

    @Override
    public long getExceptionCountThreshold() {
        return DatastoreExceptionTracker.getInstance().getExceptionCountThreshold();
    }

    @Override
    public void setExceptionCountThreshold(final long exceptionCountThreshold) {
        LOG.debug("setting the exception count threshold to: {}", exceptionCountThreshold);
        DatastoreExceptionTracker.getInstance().setExceptionCountThreshold(exceptionCountThreshold);
    }
}