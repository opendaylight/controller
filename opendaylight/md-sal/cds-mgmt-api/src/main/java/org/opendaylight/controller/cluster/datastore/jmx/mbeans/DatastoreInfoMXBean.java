/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.jmx.mbeans;

import javax.management.MXBean;

/**
 * JMX bean for general datastore info.
 *
 * @author Thomas Pantelis
 */
@MXBean
// FIXME: this only holds obsolete and Pekko-specfic information. When moving to new package also address below.
//        of AskTimeoutException: something along the lines 'transport-timeout'?
public interface DatastoreInfoMXBean {
    // FIXME: remove as soon as possible
    @Deprecated(since = "11.0.0", forRemoval = true)
    default double getTransactionCreationRateLimit() {
        return Double.NaN;
    }

    // FIXME: AskTimeoutException is a Pekko-specific term. Rename to TransportTimeouts, which can be explained to
    //        non-Java people.
    /**
     * Return the number of {@code AskTimeoutException}s encountered by the datastore.
     *
     * @return Number of exceptions encountered
     */
    long getAskTimeoutExceptionCount();

    /**
     * Reset the number of {@code AskTimeoutException}s encountered by the datastore.
     */
    void resetAskTimeoutExceptionCount();
}
