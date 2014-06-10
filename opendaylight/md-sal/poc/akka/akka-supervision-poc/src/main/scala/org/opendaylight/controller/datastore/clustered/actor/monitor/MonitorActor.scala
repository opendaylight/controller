package org.opendaylight.controller.datastore.clustered.actor.monitor

import scala.collection.immutable.HashMap

import akka.actor._

import scala.collection.immutable.HashMap
import org.opendaylight.controller.datastore.clustered.messages.Register
import org.opendaylight.controller.datastore.clustered.messages.DownShard


class MonitorActor extends Actor with ActorLogging {

  //stores the actors that are being monitored by the MonitorActor
  var monitoredActors = new HashMap[ActorRef, ActorRef]

  def receive: Receive = {
    /*
      Terminated message appears for the Actors being watched.
     */
    case t: Terminated =>
      if (monitoredActors.contains(t.actor)) {
        println("MonitorActor:Received Shard Termination Message -> "
          + t.actor.toString)
        println("MonitorActor:Sending message to Supervisor")
        val value: Option[ActorRef] = monitoredActors.get(t.actor)
        value.get ! new DownShard (t.actor.toString())
      }else{
        println ("MonitorActor:"+t.actor.toString + " not found!!")
      }
    /*
      The below case help registering an actor and its supervisor that Monitor will notify
      if the watched actor goes down
     */
    case msg:Register=>
      context.watch(msg.actor)
      monitoredActors += msg.actor -> msg.supervisor
  }
}