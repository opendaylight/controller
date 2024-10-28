/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.api;

import com.google.common.annotations.Beta;
import java.util.concurrent.Executor;
import org.apache.pekko.actor.ActorContext;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.cluster.Cluster;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.RaftActor;

/**
 * Bare minimum access to the {@link ActorContext} attached to a {@link RaftActor} required to not escape actor
 * confinement.
 */
@NonNullByDefault
public interface RaftActorAccess {
    /**
     * Creates a new local actor.
     *
     * @param props the Props used to create the actor.
     * @return a reference to the newly created actor.
     */
    ActorRef actorOf(Props props);

    /**
     * Creates an actor selection.
     *
     * @param path the path.
     * @return an actor selection for the given actor path.
     */
    ActorSelection actorSelection(String path);

    /**
     * Returns the The ActorSystem associated with this context.
     *
     * @return the ActorSystem.
     */
    ActorSystem actorSystem();

    /**
     * Return an {@link Executor} which is guaranteed to run tasks in the context of {@link RaftActor}.
     *
     * @return An executor.
     */
    Executor executor();

    /**
     * The {@link Cluster} singleton for the actor system if one is configured.
     *
     * @return a {@link Cluster} instance, or {@code null}
     */
    @Nullable Cluster cluster();

    /**
     * Returns the reference to the {@link RaftActor}. This can be used to send messages to the RaftActor.
     *
     * @return the reference to the {@link RaftActor}
     */
    // FIXME: is this really needed?
    // FIXME: currently raftActor() == shardActor() and hence it is used by the MXBean at least
    @Beta
    ActorRef raftActor();
}
