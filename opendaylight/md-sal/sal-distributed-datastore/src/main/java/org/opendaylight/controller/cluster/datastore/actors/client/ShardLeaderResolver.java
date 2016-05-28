/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import akka.actor.ActorRef;
import java.util.Map.Entry;
import java.util.concurrent.CompletionStage;
import org.opendaylight.controller.cluster.access.ABIVersion;

/**
 * Caching resolver which resolves a cookie to a leader {@link ActorRef}. This class needs to be specialized by the
 * client. It is used by {@link ClientActorBehavior} for request dispatch. Results are cached until they are invalidated
 * by either the client actor (when a message timeout is detected) and by the specific frontend (on explicit shootdown
 * or when updated information becomes available.
 *
 * @author Robert Varga
 */
public abstract class ShardLeaderResolver {
    public abstract CompletionStage<Entry<ActorRef, ABIVersion>> getShardLeader(Long cookie);

}
