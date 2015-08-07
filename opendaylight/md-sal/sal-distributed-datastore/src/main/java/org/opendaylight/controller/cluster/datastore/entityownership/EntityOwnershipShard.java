/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import akka.actor.Props;
import java.util.Map;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Special Shard for EntityOwnership.
 *
 * @author Thomas Pantelis
 */
public class EntityOwnershipShard extends Shard {

    private static DatastoreContext noPersistenceDatastoreContext(DatastoreContext datastoreContext) {
        return DatastoreContext.newBuilderFrom(datastoreContext).persistent(false).build();
    }

    protected EntityOwnershipShard(ShardIdentifier name, Map<String, String> peerAddresses,
            DatastoreContext datastoreContext, SchemaContext schemaContext) {
        super(name, peerAddresses, noPersistenceDatastoreContext(datastoreContext), schemaContext);
    }

    @Override
    protected void onDatastoreContext(DatastoreContext context) {
        super.onDatastoreContext(noPersistenceDatastoreContext(context));
    }

    public static Props props(final ShardIdentifier name, final Map<String, String> peerAddresses,
            final DatastoreContext datastoreContext, final SchemaContext schemaContext) {
        return Props.create(new Creator(name, peerAddresses, datastoreContext, schemaContext));
    }

    private static class Creator extends AbstractShardCreator {
        private static final long serialVersionUID = 1L;

        Creator(final ShardIdentifier name, final Map<String, String> peerAddresses,
                final DatastoreContext datastoreContext, final SchemaContext schemaContext) {
            super(name, peerAddresses, datastoreContext, schemaContext);
        }

        @Override
        public Shard create() throws Exception {
            return new EntityOwnershipShard(name, peerAddresses, datastoreContext, schemaContext);
        }
    }
}
