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
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;

public class ClusterWrapperImpl implements ClusterWrapper {
    private final Cluster cluster;
    private final String currentMemberName;

    public ClusterWrapperImpl(ActorSystem actorSystem){
        cluster = Cluster.get(actorSystem);
        currentMemberName = (String) cluster.getSelfRoles().toArray()[0];

    }

    public void subscribeToMemberEvents(ActorRef actorRef){
        cluster.subscribe(actorRef, ClusterEvent.initialStateAsEvents(),
            ClusterEvent.MemberEvent.class,
            ClusterEvent.UnreachableMember.class);
    }

    public String getCurrentMemberName() {
        return currentMemberName;
    }
}
