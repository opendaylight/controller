package org.opendaylight.controller

import akka.actor.Actor
import akka.event.Logging

class HelloWorld extends Actor{
  val log = Logging(context.system, this)
  override def receive = {
      case OneWayMessage(message) => log.info(message);
      case EchoRequest(message) => sender ! EchoResponse("Hello " + message)
  }

}
