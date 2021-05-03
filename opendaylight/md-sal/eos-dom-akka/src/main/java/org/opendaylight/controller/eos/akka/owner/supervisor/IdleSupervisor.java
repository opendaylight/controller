/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.supervisor;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.Member;
import akka.cluster.typed.Cluster;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.ActivateDataCenter;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.OwnerSupervisorCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initial Supervisor behavior that stays idle and only switches itself to the active behavior when its running
 * in the primary datacenter, or is activated on demand. Once the supervisor instance is no longer needed in the
 * secondary datacenter it needs to be deactivated manually.
 */
public final class IdleSupervisor extends AbstractBehavior<OwnerSupervisorCommand> {
    private static final Logger LOG = LoggerFactory.getLogger(IdleSupervisor.class);

//    private static final String PRIMARY_DATACENTER_KEY = "akka.cluster.primary-datacenter";
    private static final String DATACENTER_PREFIX = "dc-";
    private static final String DEFAULT_DATACENTER = "dc-default";

    private IdleSupervisor(final ActorContext<OwnerSupervisorCommand> context) {
        super(context);
        final Cluster cluster = Cluster.get(context.getSystem());

        final String datacenterRole = extractDatacenterRole(cluster.selfMember());
        if (datacenterRole.equals(DEFAULT_DATACENTER)) {
            LOG.debug("No datacenter configured, activating default data center");
            context.getSelf().tell(new ActivateDataCenter(null));
        }

        LOG.debug("Idle supervisor started on {}.", cluster.selfMember());
    }

    public static Behavior<OwnerSupervisorCommand> create() {

        return Behaviors.setup(IdleSupervisor::new);
    }

    @Override
    public Receive<OwnerSupervisorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ActivateDataCenter.class, this::onActivateDataCenter)
                .build();
    }

    private Behavior<OwnerSupervisorCommand> onActivateDataCenter(final ActivateDataCenter message) {
        LOG.debug("Received ActivateDataCenter command switching to syncer behavior,");
        return OwnerSyncer.create(message.getReplyTo());
    }

    private String extractDatacenterRole(final Member selfMember) {
        return selfMember.getRoles().stream().filter(role -> role.contains(DATACENTER_PREFIX))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No valid role found."));
    }
}
