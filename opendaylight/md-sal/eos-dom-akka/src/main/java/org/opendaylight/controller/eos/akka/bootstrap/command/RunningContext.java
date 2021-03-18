/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.bootstrap.command;

import static java.util.Objects.requireNonNull;

import akka.actor.typed.ActorRef;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.eos.akka.owner.checker.command.StateCheckerCommand;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.OwnerSupervisorCommand;
import org.opendaylight.controller.eos.akka.registry.candidate.command.CandidateRegistryCommand;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.TypeListenerRegistryCommand;

public final class RunningContext extends BootstrapCommand {
    private final @NonNull ActorRef<TypeListenerRegistryCommand> listenerRegistry;
    private final @NonNull ActorRef<CandidateRegistryCommand> candidateRegistry;
    private final @NonNull ActorRef<StateCheckerCommand> ownerStateChecker;
    private final @NonNull ActorRef<OwnerSupervisorCommand> ownerSupervisor;

    public RunningContext(final ActorRef<TypeListenerRegistryCommand> listenerRegistry,
                          final ActorRef<CandidateRegistryCommand> candidateRegistry,
                          final ActorRef<StateCheckerCommand> ownerStateChecker,
                          final ActorRef<OwnerSupervisorCommand> ownerSupervisor) {
        this.listenerRegistry = requireNonNull(listenerRegistry);
        this.candidateRegistry = requireNonNull(candidateRegistry);
        this.ownerStateChecker = requireNonNull(ownerStateChecker);
        this.ownerSupervisor = requireNonNull(ownerSupervisor);
    }

    public @NonNull ActorRef<TypeListenerRegistryCommand> getListenerRegistry() {
        return listenerRegistry;
    }

    public @NonNull ActorRef<CandidateRegistryCommand> getCandidateRegistry() {
        return candidateRegistry;
    }

    public @NonNull ActorRef<StateCheckerCommand> getOwnerStateChecker() {
        return ownerStateChecker;
    }

    public @NonNull ActorRef<OwnerSupervisorCommand> getOwnerSupervisor() {
        return ownerSupervisor;
    }
}
