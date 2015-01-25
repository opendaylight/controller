/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import com.google.common.base.Preconditions;

public class ClusterWrapperImpl implements ClusterWrapper {
    private final Cluster cluster;
    private final String currentMemberName;
    private final Address selfAddress;

    public ClusterWrapperImpl(ActorSystem actorSystem){
        Preconditions.checkNotNull(actorSystem, "actorSystem should not be null");

        cluster = Cluster.get(actorSystem);

        Preconditions.checkState(cluster.getSelfRoles().size() > 0,
            "No akka roles were specified\n" +
                "One way to specify the member name is to pass a property on the command line like so\n" +
                "   -Dakka.cluster.roles.0=member-3\n" +
                "member-3 here would be the name of the member"
        );

        currentMemberName = cluster.getSelfRoles().iterator().next();
        selfAddress = cluster.selfAddress();
    }

    public void subscribeToMemberEvents(ActorRef actorRef){
        Preconditions.checkNotNull(actorRef, "actorRef should not be null");

        cluster.subscribe(actorRef, ClusterEvent.initialStateAsEvents(),
            ClusterEvent.MemberEvent.class,
            ClusterEvent.UnreachableMember.class);
    }

    public String getCurrentMemberName() {
        return currentMemberName;
    }

    public Address getSelfAddress() {
        return selfAddress;
    }
}
