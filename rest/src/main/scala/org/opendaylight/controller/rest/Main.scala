package org.opendaylight.controller.rest

import akka.actor.ActorSystem
import akka.actor.Props

object Main extends App{
    println("Starting System")
	val system = ActorSystem("features-api")
	
	val restManager = system.actorOf(Props[Rest], "restManager")
	
	//val featureManager = system.actorOf(Props[FeatureManager], "featureManager")
}