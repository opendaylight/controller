package org.opendaylight.controller

import akka.persistence.Persistent
import akka.actor.ActorRef


case class OneWayMessage(text : String)

case class EchoRequest(text : String)
case class EchoResponse(text : String)


case class KeyValue(val key : String, val value : String);

case class ShardAddressByModuleName(val moduleName : String)
case class ShardFound(moduleName: String, address : String)
case class ShardNotFound(moduleName : String)
case class ShardsForMember()
case class Shards(shards : Set[String], memberName : String)

case class RegisterListener(val actor : ActorRef, val memberName : String)
case class MemberAvailable(address : String)
case class MemberUnAvailable(address : String)
case class Replicated(payLoad : Any)