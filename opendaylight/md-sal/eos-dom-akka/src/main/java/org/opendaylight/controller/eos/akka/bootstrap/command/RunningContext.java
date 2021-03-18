/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.bootstrap.command;

import akka.actor.typed.ActorRef;
import org.opendaylight.controller.eos.akka.owner.checker.command.StateCheckerCommand;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.OwnerSupervisorCommand;
import org.opendaylight.controller.eos.akka.registry.candidate.command.CandidateRegistryCommand;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.TypeListenerRegistryCommand;

public class RunningContext implements BootstrapCommand {

    private final ActorRef<TypeListenerRegistryCommand> listenerRegistry;
    private final ActorRef<CandidateRegistryCommand> candidateRegistry;
    private final ActorRef<StateCheckerCommand> ownerStateChecker;
    private final ActorRef<OwnerSupervisorCommand> ownerSupervisor;

    public RunningContext(final ActorRef<TypeListenerRegistryCommand> listenerRegistry,
                          final ActorRef<CandidateRegistryCommand> candidateRegistry,
                          final ActorRef<StateCheckerCommand> ownerStateChecker,
                          final ActorRef<OwnerSupervisorCommand> ownerSupervisor) {
        this.listenerRegistry = listenerRegistry;
        this.candidateRegistry = candidateRegistry;
        this.ownerStateChecker = ownerStateChecker;
        this.ownerSupervisor = ownerSupervisor;
    }

    public ActorRef<TypeListenerRegistryCommand> getListenerRegistry() {
        return listenerRegistry;
    }

    public ActorRef<CandidateRegistryCommand> getCandidateRegistry() {
        return candidateRegistry;
    }

    public ActorRef<StateCheckerCommand> getOwnerStateChecker() {
        return ownerStateChecker;
    }

    public ActorRef<OwnerSupervisorCommand> getOwnerSupervisor() {
        return ownerSupervisor;
    }
}
