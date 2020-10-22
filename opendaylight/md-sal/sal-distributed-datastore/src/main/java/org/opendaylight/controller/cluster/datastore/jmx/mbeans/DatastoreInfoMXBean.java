/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.jmx.mbeans;

import akka.pattern.AskTimeoutException;

/**
 * JMX bean for general datastore info.
 *
 * @author Thomas Pantelis
 */
public interface DatastoreInfoMXBean {

    double getTransactionCreationRateLimit();

    /**
     * Return the number of {@link AskTimeoutException}s encountered by the datastore.
     *
     * @return Number of exceptions encountered
     */
    long getAskTimeoutExceptionCount();

    /**
     * Reset the number of {@link AskTimeoutException}s encountered by the datastore.
     */
    void resetAskTimeoutExceptionCount();
}
