/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.eos.owner.checker;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.ddata.LWWRegister;
import akka.cluster.ddata.LWWRegisterKey;
import akka.cluster.ddata.typed.javadsl.DistributedData;
import akka.cluster.ddata.typed.javadsl.Replicator;
import akka.cluster.ddata.typed.javadsl.ReplicatorMessageAdapter;
import java.time.Duration;
import org.opendaylight.controller.cluster.akka.eos.owner.checker.command.GetOwnershipState;
import org.opendaylight.controller.cluster.akka.eos.owner.checker.command.GetOwnershipStateReply;
import org.opendaylight.controller.cluster.akka.eos.owner.checker.command.InternalGetReply;
import org.opendaylight.controller.cluster.akka.eos.owner.checker.command.StateCheckerCommand;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OwnerStateChecker extends AbstractBehavior<StateCheckerCommand> {

    private static final Logger LOG = LoggerFactory.getLogger(OwnerStateChecker.class);

    private final String localMember;
    private final ReplicatorMessageAdapter<StateCheckerCommand, LWWRegister<String>> replicatorAdapter;

    public OwnerStateChecker(final ActorContext<StateCheckerCommand> context,
                             final String localMember) {
        super(context);
        this.localMember = localMember;

        final ActorRef<Replicator.Command> replicator = DistributedData.get(context.getSystem()).replicator();

        replicatorAdapter = new ReplicatorMessageAdapter<>(context, replicator, Duration.ofSeconds(5));
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
                askReplyTo -> new Replicator.Get<>(
                        new LWWRegisterKey<>(message.getEntity().toString()),
                        new Replicator.ReadMajority(Duration.ofSeconds(5)),
                        askReplyTo),
                (reply) -> new InternalGetReply(reply, message.getEntity(), message.getReplyTo()));
        return this;
    }

    private Behavior<StateCheckerCommand> respondWithState(final InternalGetReply reply) {
        final Replicator.GetResponse<LWWRegister<String>> response = reply.getResponse();
        if (response instanceof Replicator.NotFound) {
            LOG.debug("Data for owner not found, most likely no owner has beed picked for entity: {}",
                    reply.getEntity());

            reply.getReplyTo().tell(new GetOwnershipStateReply(null));

        } else if (response instanceof Replicator.GetFailure) {
            LOG.warn("Failure retrieving data for entity: {}", reply.getEntity());

            reply.getReplyTo().tell(new GetOwnershipStateReply(null));

        } else if (response instanceof Replicator.GetSuccess) {
            final String owner =
                    ((Replicator.GetSuccess<LWWRegister<String>>) response).get(response.key()).getValue();
            LOG.debug("Data for owner received. {}, owner: {}", response, owner);

            final boolean isOwner = localMember.equals(owner);
            final boolean hasOwner = !owner.isEmpty();

            reply.getReplyTo().tell(new GetOwnershipStateReply(EntityOwnershipState.from(isOwner, hasOwner)));
        }
        return this;
    }
}
