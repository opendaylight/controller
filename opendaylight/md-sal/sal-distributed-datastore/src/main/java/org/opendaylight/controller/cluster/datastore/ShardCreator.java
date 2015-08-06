/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.Props;
import akka.japi.Creator;
import com.google.common.base.Preconditions;
import java.util.Map;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * An abstract class for creating Shard actor instances.
 *
 * @author Thomas Pantelis
 */
public abstract class ShardCreator implements Creator<Shard> {
    private static final long serialVersionUID = 1L;

    private ShardIdentifier shardId;
    private Map<String, String> peerAddresses;
    private DatastoreContext datastoreContext;
    private SchemaContext schemaContext;

    public ShardIdentifier getShardId() {
        return shardId;
    }

    public Map<String, String> getPeerAddresses() {
        return peerAddresses;
    }

    public DatastoreContext getDatastoreContext() {
        return datastoreContext;
    }

    public SchemaContext getSchemaContext() {
        return schemaContext;
    }

    public ShardCreator shardId(ShardIdentifier shardId) {
        this.shardId = shardId;
        return this;
    }

    public ShardCreator peerAddresses(Map<String, String> peerAddresses) {
        this.peerAddresses = peerAddresses;
        return this;
    }

    public ShardCreator datastoreContext(DatastoreContext datastoreContext) {
        this.datastoreContext = datastoreContext;
        return this;
    }

    public ShardCreator schemaContext(SchemaContext  schemaContext) {
        this. schemaContext =  schemaContext;
        return this;
    }

    public Props props() {
        return Props.create(this);
    }

    protected void validateInputs() {
        Preconditions.checkNotNull(shardId, "shardId should not be null");
        Preconditions.checkNotNull(peerAddresses, "peerAddresses should not be null");
        Preconditions.checkNotNull(datastoreContext, "dataStoreContext should not be null");
        Preconditions.checkNotNull(schemaContext, "schemaContext should not be null");
    }
}
