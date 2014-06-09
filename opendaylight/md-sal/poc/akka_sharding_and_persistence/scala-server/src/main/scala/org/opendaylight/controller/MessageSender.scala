package org.opendaylight.controller

import akka.actor.{DeadLetter, ActorRef, Actor, ActorLogging}
import akka.actor.Actor.Receive

import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.{AskTimeoutException, ask}
import akka.persistence.Persistent
import com.typesafe.config.ConfigFactory
import scala.collection.mutable
import org.opendaylight.controller.SendMessages
import scala.util.Random


class MessageSender(val shardManager : ActorRef) extends Actor with ActorLogging{
  var identifierGenerator = new Random();

  override def receive: Receive = {
    case SendMessages(number) => {
      sendMessages(number)
    }
  }

  def sendMessages(number : Int) {
    import context.dispatcher
    implicit val timeout = Timeout(5 seconds) // needed for `?` below

    val addressFuture = shardManager ? ShardAddressByModuleNameAndIdentifier("inventory", identifierGenerator.nextString(10))

    addressFuture onSuccess {
      case ShardNotFound(roleName) => {
        log.info(roleName + " was not found");
      }
      case ShardFound(roleName, address) => {

        val shard = context.system.actorSelection(address);

        log.info(s"Address = ${address}")

        val start = System.currentTimeMillis();

        for(i <- 1 to number) {
          val persistentFuture = shard ? Persistent(KeyValue("name " + i, "foobar " + i))

          persistentFuture onSuccess {
            case response: PersistedToShard => {
              println("done")
            }
          }

          persistentFuture onFailure ({
            case timeout: AskTimeoutException => {
              println(timeout)
            }
          })
        }

        // The purpose of this message is just to determine how much time message processing took
        val shardFuture = shard ? EchoRequest("processing")

        shardFuture onSuccess {
          case EchoResponse(message) => {
            val end = System.currentTimeMillis();
            println("Done in " + (end - start) + "ms")
          }
        }

      }
    }

  }
}
