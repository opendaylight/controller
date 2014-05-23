package org.opendaylight.controller.rest

import akka.actor.{ Props, ActorSystem }
import org.osgi.framework.BundleContext
import akka.osgi.ActorSystemActivator

class Activator extends ActirSystenActivator {

    def configure(context: BundleContext, system: ActorSystem) {
        // optionally register the ActorSystem in the OSGi Service Registry
        registerService(context, system)

        val rest = system.actorOf(Props[Rest], name = "rest")
        someActor ! SomeMessage
    }

    def start(context: BundleContext) {
      println("REST Service started")
      val system = ActorSystem("features-api")
/*
      // Initialize REST Manager
      val rest = system.actorOf(Props[Rest], name = "rest")

      }
*/
    def stop(context: BundleContext) {}
}