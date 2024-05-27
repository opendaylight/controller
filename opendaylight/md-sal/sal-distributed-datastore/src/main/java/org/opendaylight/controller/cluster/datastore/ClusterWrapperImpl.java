/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Address;
import org.apache.pekko.cluster.Cluster;
import org.apache.pekko.cluster.ClusterEvent;
import org.opendaylight.controller.cluster.access.concepts.MemberName;

public class ClusterWrapperImpl implements ClusterWrapper {
    private final Cluster cluster;
    private final MemberName currentMemberName;
    private final Address selfAddress;

    public ClusterWrapperImpl(final ActorSystem actorSystem) {
        cluster = Cluster.get(requireNonNull(actorSystem, "actorSystem should not be null"));

        checkState(cluster.getSelfRoles().size() > 0,
            "No pekko roles were specified.\n"
            + "One way to specify the member name is to pass a property on the command line like so\n"
            + "   -Dpekko.cluster.roles.0=member-3\n"
            + "member-3 here would be the name of the member");

        currentMemberName = MemberName.forName(cluster.getSelfRoles().iterator().next());
        selfAddress = cluster.selfAddress();
    }

    @Override
    public void subscribeToMemberEvents(final ActorRef actorRef) {
        cluster.subscribe(requireNonNull(actorRef, "actorRef should not be null"), ClusterEvent.initialStateAsEvents(),
            ClusterEvent.MemberEvent.class,
            ClusterEvent.UnreachableMember.class,
            ClusterEvent.ReachableMember.class);
    }

    @Override
    public MemberName getCurrentMemberName() {
        return currentMemberName;
    }

    @Override
    public Address getSelfAddress() {
        return selfAddress;
    }
}
