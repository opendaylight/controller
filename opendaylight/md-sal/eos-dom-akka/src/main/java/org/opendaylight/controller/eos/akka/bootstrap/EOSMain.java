/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.bootstrap;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.SupervisorStrategy;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.apache.pekko.cluster.typed.Cluster;
import org.apache.pekko.cluster.typed.ClusterSingleton;
import org.apache.pekko.cluster.typed.SingletonActor;
import org.opendaylight.controller.eos.akka.bootstrap.command.BootstrapCommand;
import org.opendaylight.controller.eos.akka.bootstrap.command.GetRunningContext;
import org.opendaylight.controller.eos.akka.bootstrap.command.RunningContext;
import org.opendaylight.controller.eos.akka.bootstrap.command.Terminate;
import org.opendaylight.controller.eos.akka.owner.checker.OwnerStateChecker;
import org.opendaylight.controller.eos.akka.owner.checker.command.StateCheckerCommand;
import org.opendaylight.controller.eos.akka.owner.supervisor.IdleSupervisor;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.OwnerSupervisorCommand;
import org.opendaylight.controller.eos.akka.registry.candidate.CandidateRegistryInit;
import org.opendaylight.controller.eos.akka.registry.candidate.command.CandidateRegistryCommand;
import org.opendaylight.controller.eos.akka.registry.listener.type.EntityTypeListenerRegistry;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.TypeListenerRegistryCommand;
import org.opendaylight.yangtools.binding.data.codec.api.BindingInstanceIdentifierCodec;
import org.opendaylight.yangtools.yang.common.Empty;

public final class EOSMain extends AbstractBehavior<BootstrapCommand> {
    private final ActorRef<TypeListenerRegistryCommand> listenerRegistry;
    private final ActorRef<CandidateRegistryCommand> candidateRegistry;
    private final ActorRef<OwnerSupervisorCommand> ownerSupervisor;
    private final ActorRef<StateCheckerCommand> ownerStateChecker;

    private EOSMain(final ActorContext<BootstrapCommand> context, final BindingInstanceIdentifierCodec iidCodec) {
        super(context);

        final String role = Cluster.get(context.getSystem()).selfMember().getRoles().iterator().next();

        listenerRegistry = context.spawn(EntityTypeListenerRegistry.create(role), "ListenerRegistry");

        final ClusterSingleton clusterSingleton = ClusterSingleton.get(context.getSystem());
        // start the initial sync behavior that switches to the regular one after syncing
        ownerSupervisor = clusterSingleton.init(
                SingletonActor.of(Behaviors.supervise(IdleSupervisor.create(iidCodec))
                        .onFailure(SupervisorStrategy.restart()), "OwnerSupervisor"));
        candidateRegistry = context.spawn(CandidateRegistryInit.create(ownerSupervisor), "CandidateRegistry");

        ownerStateChecker = context.spawn(OwnerStateChecker.create(role, ownerSupervisor, iidCodec),
                "OwnerStateChecker");
    }

    public static Behavior<BootstrapCommand> create(final BindingInstanceIdentifierCodec iidCodec) {
        return Behaviors.setup(context -> new EOSMain(context, iidCodec));
    }

    @Override
    public Receive<BootstrapCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(GetRunningContext.class, this::onGetRunningContext)
                .onMessage(Terminate.class, this::onTerminate)
                .build();
    }

    private Behavior<BootstrapCommand> onGetRunningContext(final GetRunningContext request) {
        request.getReplyTo().tell(
                new RunningContext(listenerRegistry, candidateRegistry, ownerStateChecker, ownerSupervisor));
        return this;
    }

    private Behavior<BootstrapCommand> onTerminate(final Terminate request) {
        request.getReplyTo().tell(Empty.value());
        return Behaviors.stopped();
    }
}
