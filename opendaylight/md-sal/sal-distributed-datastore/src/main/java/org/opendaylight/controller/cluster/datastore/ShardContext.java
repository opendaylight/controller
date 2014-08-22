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
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import scala.concurrent.duration.Duration;

/**
 * Contains contextual data for shards.
 *
 * @author Thomas Pantelis
 */
public class ShardContext {

    private final InMemoryDOMDataStoreConfigProperties dataStoreProperties;
    private Duration shardTransactionIdleTimeout = Duration.create(10, TimeUnit.MINUTES);
    private volatile SchemaContext schemaContext;

    public ShardContext(InMemoryDOMDataStoreConfigProperties dataStoreProperties) {
        this.dataStoreProperties = dataStoreProperties;
    }

    public InMemoryDOMDataStoreConfigProperties getDataStoreProperties() {
        return dataStoreProperties;
    }

    public Duration getShardTransactionIdleTimeout() {
        return shardTransactionIdleTimeout;
    }

    public void setShardTransactionIdleTimeout(Duration shardTransactionIdleTimeout) {
        this.shardTransactionIdleTimeout = shardTransactionIdleTimeout;
    }

    public SchemaContext getSchemaContext() {
        return schemaContext;
    }

    public void setSchemaContext(SchemaContext schemaContext) {
        this.schemaContext = schemaContext;
    }
}
