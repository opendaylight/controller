/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;

import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreConfigProperties;

import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

/**
 * Contains contextual data for a data store.
 *
 * @author Thomas Pantelis
 */
public class DatastoreContext {

    private final InMemoryDOMDataStoreConfigProperties dataStoreProperties;
    private final Duration shardTransactionIdleTimeout;
    private final String dataStoreMXBeanType;

    public DatastoreContext() {
        this.dataStoreProperties = null;
        this.dataStoreMXBeanType = "DistributedDatastore";
        this.shardTransactionIdleTimeout = Duration.create(10, TimeUnit.MINUTES);
    }

    public DatastoreContext(String dataStoreMXBeanType,
            InMemoryDOMDataStoreConfigProperties dataStoreProperties,
            Duration shardTransactionIdleTimeout) {
        this.dataStoreMXBeanType = dataStoreMXBeanType;
        this.dataStoreProperties = Preconditions.checkNotNull(dataStoreProperties);
        this.shardTransactionIdleTimeout = Preconditions.checkNotNull(shardTransactionIdleTimeout);
    }

    public InMemoryDOMDataStoreConfigProperties getDataStoreProperties() {
        return dataStoreProperties;
    }

    public Duration getShardTransactionIdleTimeout() {
        return shardTransactionIdleTimeout;
    }

    public String getDataStoreMXBeanType() {
        return dataStoreMXBeanType;
    }
}
