/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreConfigProperties;
import com.google.common.base.Preconditions;

import scala.concurrent.duration.Duration;

/**
 * Contains contextual data for shards.
 *
 * @author Thomas Pantelis
 */
public class ShardContext {

    private final InMemoryDOMDataStoreConfigProperties dataStoreProperties;
    private final Duration shardTransactionIdleTimeout;

    public ShardContext() {
        this.dataStoreProperties = null;
        this.shardTransactionIdleTimeout = Duration.create(10, TimeUnit.MINUTES);
    }

    public ShardContext(InMemoryDOMDataStoreConfigProperties dataStoreProperties,
            Duration shardTransactionIdleTimeout) {
        this.dataStoreProperties = Preconditions.checkNotNull(dataStoreProperties);
        this.shardTransactionIdleTimeout = Preconditions.checkNotNull(shardTransactionIdleTimeout);
    }

    public InMemoryDOMDataStoreConfigProperties getDataStoreProperties() {
        return dataStoreProperties;
    }

    public Duration getShardTransactionIdleTimeout() {
        return shardTransactionIdleTimeout;
    }
}
