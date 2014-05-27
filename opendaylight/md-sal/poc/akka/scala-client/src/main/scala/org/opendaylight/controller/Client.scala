package org.opendaylight.controller

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global
import akka.persistence.Persistent
import com.typesafe.config.{Config, ConfigFactory}


object Client {
  val system = ActorSystem("opendaylight");
  var clusteringServiceProps = Props(new ClusteringService())
  var clusteringServiceActor = system.actorOf(clusteringServiceProps, "clustering-service" )

  def main(args : Array[String]) {
    var ok = true
    while (ok) {
      print("Enter 1 to send messages, 2 to get messages : ")
      val ln = readLine()
      ok = ln != null
      if(ln == "1"){
        println("Sending Messages to the server. Please wait for a completion message.")
        sendMessages
      } else {
        println("Sorry didn't get that")
      }

    }
  }

  def sendMessages {
    implicit val timeout = Timeout(5 seconds) // needed for `?` below

    // Find the address of a shard given it's 'yang' module name. This can get as sophisticated as we want
    val addressFuture = clusteringServiceActor ? ShardAddressByModuleName("opendaylight-inventory")

    addressFuture onSuccess {
      case ShardNotFound(roleName) => {
        println(roleName + " was not found");
      }
      case ShardFound(roleName, address) => {

        val shard = system.actorSelection(address);

        val start = System.currentTimeMillis();

        for(i <- 0 to 10) {
          shard ! Persistent(KeyValue("name " + i, "foobar " + i))
        }

        val shardFuture = shard ? EchoRequest("processing")

        shardFuture onSuccess {
          case EchoResponse(message) => {
            val end = System.currentTimeMillis();
            println("Done in " + (end-start) + "ms")
          }
        }

      }
    }


  }
}
