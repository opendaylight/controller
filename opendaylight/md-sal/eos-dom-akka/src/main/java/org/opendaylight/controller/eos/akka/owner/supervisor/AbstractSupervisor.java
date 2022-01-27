/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.supervisor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.cluster.ddata.ORMap;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.typed.javadsl.DistributedData;
import akka.cluster.ddata.typed.javadsl.Replicator;
import akka.cluster.ddata.typed.javadsl.ReplicatorMessageAdapter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.ClearCandidates;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.ClearCandidatesForMember;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.ClearCandidatesResponse;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.OwnerSupervisorCommand;
import org.opendaylight.controller.eos.akka.registry.candidate.CandidateRegistry;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.slf4j.Logger;

abstract class AbstractSupervisor extends AbstractBehavior<OwnerSupervisorCommand> {

    final ReplicatorMessageAdapter<OwnerSupervisorCommand, ORMap<DOMEntity, ORSet<String>>> candidateReplicator;

    @SuppressFBWarnings(value = "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR",
        justification = "getContext() is non-final")
    AbstractSupervisor(final ActorContext<OwnerSupervisorCommand> context) {
        super(context);

        final ActorRef<Replicator.Command> replicator = DistributedData.get(getContext().getSystem()).replicator();
        candidateReplicator = new ReplicatorMessageAdapter<>(getContext(), replicator, Duration.ofSeconds(5));
    }

    Behavior<OwnerSupervisorCommand> onClearCandidatesForMember(final ClearCandidatesForMember command) {
        getLogger().debug("Clearing candidates for member: {}", command.getCandidate());

        candidateReplicator.askGet(
                askReplyTo -> new Replicator.Get<>(CandidateRegistry.KEY,
                        new Replicator.ReadMajority(Duration.ofSeconds(15)), askReplyTo),
                response -> new ClearCandidates(response, command));

        return this;
    }

    Behavior<OwnerSupervisorCommand> finishClearCandidates(final ClearCandidates command) {
        if (command.getResponse() instanceof Replicator.GetSuccess) {
            getLogger().debug("Retrieved candidate data, clearing candidates for {}",
                    command.getOriginalMessage().getCandidate());

            getContext().spawnAnonymous(CandidateCleaner.create()).tell(command);
        } else {
            getLogger().debug("Unable to retrieve candidate data for {}, no candidates present sending empty reply",
                    command.getOriginalMessage().getCandidate());
            command.getOriginalMessage().getReplyTo().tell(new ClearCandidatesResponse());
        }

        return this;
    }

    abstract Logger getLogger();
}
