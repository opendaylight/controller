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
 * An interface for creating a Shard actor Props instance.
 *
 * @author Thomas Pantelis
 */
public interface ShardPropsCreator {

    Props newProps(ShardIdentifier shardId, Map<String, String> peerAddresses, DatastoreContext datastoreContext,
            SchemaContext schemaContext);
}
