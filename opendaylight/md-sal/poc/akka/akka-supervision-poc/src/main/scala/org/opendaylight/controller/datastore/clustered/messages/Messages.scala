package org.opendaylight.controller.datastore.clustered.messages
import akka.actor.ActorRef


case class DownShard(text:String)
case class Register(val actor: ActorRef, val supervisor: ActorRef)

//Messages send to ShardManager
case class ResetShards()
case class Persist(val numberOfFamilies:Int,val numberOfRun:Int)
case class Output()

//Exception messages
case class SimulateResume()
case class SimulateRestart()

//Shutdown
case class Shutdown()
