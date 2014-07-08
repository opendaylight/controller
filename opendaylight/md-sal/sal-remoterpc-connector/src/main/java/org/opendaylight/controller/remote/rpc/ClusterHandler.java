/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;


import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;

import java.util.Iterator;
import java.util.Random;


public class ClusterHandler {

  private Cluster cluster;
  private ActorSystem actorSystem;

  public ClusterHandler(ActorSystem actorSystem) {
    cluster = Cluster.get(actorSystem);
    this.actorSystem = actorSystem;
  }

  public ActorSelection getRandomRegistryActor() {
    ClusterEvent.CurrentClusterState clusterState = cluster.state();
    int index = new Random().nextInt(clusterState.members().size());
    Iterator<Member> iterator = clusterState.getMembers().iterator();
    int i = 0;
    ActorSelection actor = null;
    while(iterator.hasNext()) {
      if(i == index) {
        Member member = iterator.next();
        actor = actorSystem.actorSelection(member.address()+ "/user/rpcRegistry");
        break;
      }
      i++;
    }
    return actor;
  }
}
