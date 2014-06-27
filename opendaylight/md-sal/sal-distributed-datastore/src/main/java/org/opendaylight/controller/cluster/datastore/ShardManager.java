/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryNotFound;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The ShardManager has the following jobs,
 * <p>
 * <li> Create all the local shard replicas that belong on this cluster member
 * <li> Find the primary replica for any given shard
 * <li> Engage in shard replica elections which decide which replica should be the primary
 * </p>
 * <p/>
 * <h3>>Creation of Shard replicas</h3
 * <p>
 * When the ShardManager is constructed it reads the cluster configuration to find out which shard replicas
 * belong on this member. It finds out the name of the current cluster member from the Akka Clustering Service.
 * </p>
 * <p/>
 * <h3> Replica Elections </h3>
 * <p/>
 * <p>
 * The Shard Manager uses multiple cues to initiate election.
 * <li> When a member of the cluster dies
 * <li> When a local shard replica dies
 * <li> When a local shard replica comes alive
 * </p>
 */
public class ShardManager extends AbstractUntypedActor {

  // Stores a mapping between a shard name and the address of the current primary
  private final Map<String, Address> shardNameToPrimaryAddress = new HashMap<>();

  // Stores a mapping between a member name and the address of the member
  private final Map<String, Address> memberNameToAddress = new HashMap<>();

  // Stores a mapping between the shard name and all the members on which a replica of that shard are available
  private final Map<String, List<String>> shardNameToMembers = new HashMap<>();

  private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  private final ActorPath defaultShardPath;

  /**
   *
   * @param type defines the kind of data that goes into shards created by this shard manager. Examples of type would be
   *             configuration or operational
   */
  private ShardManager(String type){
    ActorRef actor = getContext().actorOf(Shard.props("shard-" + Shard.DEFAULT_NAME + "-" + type), "shard-" + Shard.DEFAULT_NAME + "-" + type);
    defaultShardPath = actor.path();
  }

  public static Props props(final String type){
    return Props.create(new Creator<ShardManager>(){

      @Override
      public ShardManager create() throws Exception {
        return new ShardManager(type);
      }
    });
  }

  @Override
  public void handleReceive(Object message) throws Exception {
    if (message instanceof FindPrimary) {
      FindPrimary msg = ((FindPrimary) message);
      String shardName = msg.getShardName();
      if(Shard.DEFAULT_NAME.equals(shardName)){
        getSender().tell(new PrimaryFound(defaultShardPath.toString()), getSelf());
      } else {
        getSender().tell(new PrimaryNotFound(shardName), getSelf());
      }
    } else if(message instanceof UpdateSchemaContext){
        // FIXME : Notify all local shards of a context change
        getContext().system().actorSelection(defaultShardPath).forward(message, getContext());
    }
  }


}
