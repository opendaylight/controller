package org.opendaylight.controller

import akka.actor.Actor
import akka.event.Logging

/** HelloWorld is a simple Actor which can handle both a tell and a ask type message
  *
  */
class HelloWorld extends Actor{
  val log = Logging(context.system, this)
  override def receive = {
      case OneWayMessage(message) => log.info(message);
      case EchoRequest(message) => sender ! EchoResponse("Hello " + message)
  }

}
