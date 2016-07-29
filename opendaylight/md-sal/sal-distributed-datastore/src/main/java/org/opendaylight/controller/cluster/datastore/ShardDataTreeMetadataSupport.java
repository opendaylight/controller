/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;

/**
 * Base class for objects which need to enrich {@link ShardDataTree} payloads and snapshots with additional metadata.
 * This is useful for flooding state data from leader to followers such that when a transition from being a follower
 * to being a leader occurs, the new leader is seeded with state from this metadata.
 *
 * @author Robert Varga
 */
@NotThreadSafe
abstract class ShardDataTreeMetadataSupport {

    /**
     * Apply metadata tracking information from an incoming payload.
     *
     * @param payload
     */
    abstract void applyIncomingPayload(Payload payload);

    /**
     *
     */
    abstract MetadataSnapshot takeSnapshot();
}
