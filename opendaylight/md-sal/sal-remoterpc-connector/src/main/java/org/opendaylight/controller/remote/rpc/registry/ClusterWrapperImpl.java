/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.registry;


import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;


public class ClusterWrapperImpl  implements ClusterWrapper{

  private Cluster cluster;

  public ClusterWrapperImpl(ActorSystem actorSystem) {
    cluster = Cluster.get(actorSystem);
  }

  @Override
  public ClusterEvent.CurrentClusterState getState() {
    return cluster.state();
  }

  @Override
  public Address getAddress() {
    return cluster.selfAddress();
  }
}
