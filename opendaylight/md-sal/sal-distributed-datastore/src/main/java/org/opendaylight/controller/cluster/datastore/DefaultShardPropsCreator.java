/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.Props;
import java.util.Map;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Implementation of ShardPropsCreator that creates a Props instance for the Shard class.
 *
 * @author Thomas Pantelis
 */
public class DefaultShardPropsCreator implements ShardPropsCreator {

    @Override
    public Props newProps(ShardIdentifier shardId, Map<String, String> peerAddresses,
            DatastoreContext datastoreContext, SchemaContext schemaContext) {
        return Shard.props(shardId, peerAddresses, datastoreContext, schemaContext);
    }
}
