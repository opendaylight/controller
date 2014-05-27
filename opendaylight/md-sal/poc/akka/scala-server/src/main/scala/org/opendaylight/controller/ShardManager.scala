package org.opendaylight.controller

import akka.actor.{Props, ActorRef, Actor, ActorLogging}
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global


class ShardManager(val clusteringServiceActor : ActorRef) extends Actor with ActorLogging{

  implicit val timeout = Timeout(5 seconds) // needed for `?` below

  val future = clusteringServiceActor ? ShardsForMember

  future onSuccess {
    case Shards(localShards, clusterMemberName) => {
      localShards.foreach((shardName : String) => {
        val members = ConfigurationUtils.membersByShardName(shardName)
        context.actorOf(Props(new KVShard(clusterMemberName, shardName, members, clusteringServiceActor)), shardName);
      })
    }
  }

  override def receive: Receive = {
    case _ => {
      log.info("Don't know how to handle")
    }
  }
}
