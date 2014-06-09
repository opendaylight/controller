package org.opendaylight.controller

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global
import akka.persistence.Persistent
import com.typesafe.config.{Config, ConfigFactory}
import scala.sys.SystemProperties



object Client {
  val system = ActorSystem("opendaylight");
  val messageSenderAddress = new SystemProperties().get("message-sender")
  var messageSender = system.actorSelection(messageSenderAddress.get);

  def main(args : Array[String]) {
    var ok = true
    while (ok) {
      print("Enter 1 to send messages, 2 to get messages : ")
      val ln = readLine()
      ok = ln != null
      if(ln == "1"){
        println("Sending Messages to the server. Please wait for a completion message.")
        messageSender ! SendMessages(10)
      } else {
        println("Sorry didn't get that")
      }

    }
  }


}
