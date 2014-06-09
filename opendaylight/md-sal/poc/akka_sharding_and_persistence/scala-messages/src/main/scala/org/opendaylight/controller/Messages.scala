package org.opendaylight.controller

import akka.persistence.Persistent
import akka.actor.{ActorPath, Address, ActorRef}

// Message to test 'tell'
case class OneWayMessage(text : String)

// Messages to test 'ask'
case class EchoRequest(text : String)
case class EchoResponse(text : String)

// A KeyValue Message
case class KeyValue(val key : String, val value : String);

// Find the Address of a Shard given it's module name
case class ShardAddressByModuleName(val moduleName : String)
case class ShardAddressByModuleNameAndIdentifier(val moduleName : String, val identifier : String)
case class ShardFound(moduleName: String, address : String)
case class ShardNotFound(moduleName : String)

// Find all the local shards
case class ShardsForMember()
case class Shards(shards : Set[(String, String)], memberName : String)

// Register a listener
case class RegisterListener(val actorPath : ActorPath, val memberName : String)

// Message that acts as a wrapper around a Persistent payload
case class Replicated(payLoad : Any)

// Message that indicates that a persistent message was persisted to a shard
case class PersistedToShard()

// Find the address of a member given a member name
case class MemberAddressByName(val memberName : String)
case class MemberAddress(val address : String)

case class SendMessages(val number: Int)