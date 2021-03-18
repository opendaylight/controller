/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.bootstrap;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.ClusterSingleton;
import akka.cluster.typed.SingletonActor;
import org.opendaylight.controller.eos.akka.bootstrap.command.BootstrapCommand;
import org.opendaylight.controller.eos.akka.bootstrap.command.GetRunningContext;
import org.opendaylight.controller.eos.akka.bootstrap.command.RunningContext;
import org.opendaylight.controller.eos.akka.bootstrap.command.Terminate;
import org.opendaylight.controller.eos.akka.owner.checker.OwnerStateChecker;
import org.opendaylight.controller.eos.akka.owner.checker.command.StateCheckerCommand;
import org.opendaylight.controller.eos.akka.owner.supervisor.OwnerSyncer;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.OwnerSupervisorCommand;
import org.opendaylight.controller.eos.akka.registry.candidate.CandidateRegistry;
import org.opendaylight.controller.eos.akka.registry.candidate.command.CandidateRegistryCommand;
import org.opendaylight.controller.eos.akka.registry.listener.type.EntityTypeListenerRegistry;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.TypeListenerRegistryCommand;
import org.opendaylight.yangtools.yang.common.Empty;

public final class EOSMain extends AbstractBehavior<BootstrapCommand> {
    private final ActorRef<TypeListenerRegistryCommand> listenerRegistry;
    private final ActorRef<CandidateRegistryCommand> candidateRegistry;
    private final ActorRef<OwnerSupervisorCommand> ownerSupervisor;
    private final ActorRef<StateCheckerCommand> ownerStateChecker;

    private EOSMain(final ActorContext<BootstrapCommand> context) {
        super(context);

        final String role = Cluster.get(context.getSystem()).selfMember().getRoles().iterator().next();

        listenerRegistry = context.spawn(EntityTypeListenerRegistry.create(role), "ListenerRegistry");
        candidateRegistry = context.spawn(CandidateRegistry.create(), "CandidateRegistry");
        ownerStateChecker = context.spawn(OwnerStateChecker.create(role), "OwnerStateChecker");

        final ClusterSingleton clusterSingleton = ClusterSingleton.get(context.getSystem());
        // start the initial sync behavior that switches to the regular one after syncing
        ownerSupervisor = clusterSingleton.init(SingletonActor.of(OwnerSyncer.create(), "OwnerSupervisor"));
    }

    public static Behavior<BootstrapCommand> create() {
        return Behaviors.setup(EOSMain::new);
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
        request.getReplyTo().tell(Empty.getInstance());
        return Behaviors.stopped();
    }
}
