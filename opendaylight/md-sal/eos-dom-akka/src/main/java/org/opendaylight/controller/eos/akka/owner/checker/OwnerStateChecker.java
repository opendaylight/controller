/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.checker;

import static java.util.Objects.requireNonNull;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.ddata.LWWRegister;
import akka.cluster.ddata.LWWRegisterKey;
import akka.cluster.ddata.typed.javadsl.DistributedData;
import akka.cluster.ddata.typed.javadsl.Replicator.Get;
import akka.cluster.ddata.typed.javadsl.Replicator.GetFailure;
import akka.cluster.ddata.typed.javadsl.Replicator.GetResponse;
import akka.cluster.ddata.typed.javadsl.Replicator.GetSuccess;
import akka.cluster.ddata.typed.javadsl.Replicator.NotFound;
import akka.cluster.ddata.typed.javadsl.Replicator.ReadMajority;
import akka.cluster.ddata.typed.javadsl.ReplicatorMessageAdapter;
import java.time.Duration;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetOwnershipState;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetOwnershipStateReply;
import org.opendaylight.controller.eos.akka.owner.checker.command.InternalGetReply;
import org.opendaylight.controller.eos.akka.owner.checker.command.StateCheckerCommand;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OwnerStateChecker extends AbstractBehavior<StateCheckerCommand> {
    private static final Logger LOG = LoggerFactory.getLogger(OwnerStateChecker.class);
    private static final Duration GET_OWNERSHIP_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration UNEXPECTED_ASK_TIMEOUT = Duration.ofSeconds(5);

    private final ReplicatorMessageAdapter<StateCheckerCommand, LWWRegister<String>> replicatorAdapter;
    private final String localMember;

    private OwnerStateChecker(final ActorContext<StateCheckerCommand> context, final String localMember) {
        super(context);
        this.localMember = requireNonNull(localMember);
        replicatorAdapter = new ReplicatorMessageAdapter<>(context,
            DistributedData.get(context.getSystem()).replicator(), UNEXPECTED_ASK_TIMEOUT);
    }

    public static Behavior<StateCheckerCommand> create(final String localMember) {
        return Behaviors.setup(ctx -> new OwnerStateChecker(ctx, localMember));
    }

    @Override
    public Receive<StateCheckerCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(GetOwnershipState.class, this::onGetOwnershipState)
                .onMessage(InternalGetReply.class, this::respondWithState)
                .build();
    }

    private Behavior<StateCheckerCommand> onGetOwnershipState(final GetOwnershipState message) {
        replicatorAdapter.askGet(
                askReplyTo -> new Get<>(
                        new LWWRegisterKey<>(message.getEntity().toString()),
                        new ReadMajority(GET_OWNERSHIP_TIMEOUT),
                        askReplyTo),
                reply -> new InternalGetReply(reply, message.getEntity(), message.getReplyTo()));
        return this;
    }

    private Behavior<StateCheckerCommand> respondWithState(final InternalGetReply reply) {
        final GetResponse<LWWRegister<String>> response = reply.getResponse();
        if (response instanceof NotFound) {
            LOG.debug("Data for owner not found, most likely no owner has beed picked for entity: {}",
                    reply.getEntity());
            reply.getReplyTo().tell(new GetOwnershipStateReply(null));
        } else if (response instanceof GetFailure) {
            LOG.warn("Failure retrieving data for entity: {}", reply.getEntity());
            reply.getReplyTo().tell(new GetOwnershipStateReply(null));
        } else if (response instanceof GetSuccess) {
            final String owner = ((GetSuccess<LWWRegister<String>>) response).get(response.key()).getValue();
            LOG.debug("Data for owner received. {}, owner: {}", response, owner);

            final boolean isOwner = localMember.equals(owner);
            final boolean hasOwner = !owner.isEmpty();

            reply.getReplyTo().tell(new GetOwnershipStateReply(EntityOwnershipState.from(isOwner, hasOwner)));
        }
        return this;
    }
}
