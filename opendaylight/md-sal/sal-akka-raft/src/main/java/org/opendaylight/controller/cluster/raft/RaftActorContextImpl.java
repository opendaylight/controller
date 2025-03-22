/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorContext;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.ActorSystem;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.spi.DataPersistenceProvider;

/**
 * Implementation of the RaftActorContext interface.
 *
 * @author Moiz Raja
 * @author Thomas Pantelis
 */
final class RaftActorContextImpl extends AbstractRaftActorContext {
    private final @NonNull ActorRef actor;
    private final @NonNull ActorContext context;

    RaftActorContextImpl(final ActorRef actor, final ActorContext context, final @NonNull LocalAccess localStore,
            final @NonNull Map<String, String> peerAddresses, final @NonNull ConfigParams configParams,
            final short payloadVersion, final @NonNull DataPersistenceProvider persistenceProvider,
            final @NonNull Consumer<ApplyState> applyStateConsumer, final @NonNull Executor executor) {
        super(localStore, peerAddresses, configParams, payloadVersion, persistenceProvider, applyStateConsumer,
            executor);
        this.actor = requireNonNull(actor);
        this.context = requireNonNull(context);
    }

    @Override
    public ActorRef getActor() {
        return actor;
    }

    @Override
    public ActorSystem getActorSystem() {
        return context.system();
    }

    @Override
    ActorSelection actorSelection(final String path) {
        return context.actorSelection(path);
    }
}
