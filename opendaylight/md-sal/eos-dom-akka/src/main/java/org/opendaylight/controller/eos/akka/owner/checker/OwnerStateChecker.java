/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.checker;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.apache.pekko.cluster.ddata.LWWRegister;
import org.apache.pekko.cluster.ddata.LWWRegisterKey;
import org.apache.pekko.cluster.ddata.typed.javadsl.DistributedData;
import org.apache.pekko.cluster.ddata.typed.javadsl.Replicator;
import org.apache.pekko.cluster.ddata.typed.javadsl.Replicator.Get;
import org.apache.pekko.cluster.ddata.typed.javadsl.Replicator.GetFailure;
import org.apache.pekko.cluster.ddata.typed.javadsl.Replicator.GetResponse;
import org.apache.pekko.cluster.ddata.typed.javadsl.Replicator.GetSuccess;
import org.apache.pekko.cluster.ddata.typed.javadsl.Replicator.NotFound;
import org.apache.pekko.cluster.ddata.typed.javadsl.Replicator.ReadMajority;
import org.apache.pekko.cluster.ddata.typed.javadsl.ReplicatorMessageAdapter;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetEntitiesRequest;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetEntityOwnerRequest;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetEntityRequest;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetOwnershipState;
import org.opendaylight.controller.eos.akka.owner.checker.command.GetOwnershipStateReply;
import org.opendaylight.controller.eos.akka.owner.checker.command.InternalGetReply;
import org.opendaylight.controller.eos.akka.owner.checker.command.StateCheckerCommand;
import org.opendaylight.controller.eos.akka.owner.checker.command.StateCheckerRequest;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.OwnerSupervisorCommand;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipState;
import org.opendaylight.yangtools.binding.data.codec.api.BindingInstanceIdentifierCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OwnerStateChecker extends AbstractBehavior<StateCheckerCommand> {
    private static final Logger LOG = LoggerFactory.getLogger(OwnerStateChecker.class);
    private static final Duration GET_OWNERSHIP_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration UNEXPECTED_ASK_TIMEOUT = Duration.ofSeconds(5);

    private final ReplicatorMessageAdapter<StateCheckerCommand, LWWRegister<String>> ownerReplicator;
    private final ActorRef<OwnerSupervisorCommand> ownerSupervisor;
    private final BindingInstanceIdentifierCodec iidCodec;
    private final ActorRef<Replicator.Command> replicator;
    private final String localMember;

    private OwnerStateChecker(final ActorContext<StateCheckerCommand> context,
                              final String localMember,
                              final ActorRef<OwnerSupervisorCommand> ownerSupervisor,
                              final BindingInstanceIdentifierCodec iidCodec) {
        super(context);
        this.localMember = requireNonNull(localMember);
        this.ownerSupervisor = requireNonNull(ownerSupervisor);
        this.iidCodec = requireNonNull(iidCodec);
        replicator = DistributedData.get(context.getSystem()).replicator();
        ownerReplicator = new ReplicatorMessageAdapter<>(context, replicator, UNEXPECTED_ASK_TIMEOUT);
    }

    public static Behavior<StateCheckerCommand> create(final String localMember,
                                                       final ActorRef<OwnerSupervisorCommand> ownerSupervisor,
                                                       final BindingInstanceIdentifierCodec iidCodec) {
        return Behaviors.setup(ctx -> new OwnerStateChecker(ctx, localMember, ownerSupervisor, iidCodec));
    }

    @Override
    public Receive<StateCheckerCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(GetOwnershipState.class, this::onGetOwnershipState)
                .onMessage(InternalGetReply.class, this::respondWithState)
                .onMessage(GetEntitiesRequest.class, this::executeEntityRpc)
                .onMessage(GetEntityRequest.class, this::executeEntityRpc)
                .onMessage(GetEntityOwnerRequest.class, this::executeEntityRpc)
                .build();
    }

    private Behavior<StateCheckerCommand> onGetOwnershipState(final GetOwnershipState message) {
        ownerReplicator.askGet(
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

    private Behavior<StateCheckerCommand> executeEntityRpc(final StateCheckerRequest request) {
        final ActorRef<StateCheckerCommand> rpcHandler =
                getContext().spawnAnonymous(EntityRpcHandler.create(ownerSupervisor, iidCodec));

        LOG.debug("Executing entity rpc: {} in actor: {}", request, rpcHandler);
        rpcHandler.tell(request);
        return this;
    }
}
