/*
 * Copyright Inocybe Technlogies, 2014.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.rest

import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import akka.actor.ActorSystem
import akka.actor.Props
import akka.osgi.ActorSystemActivator


class Activator extends BundleActivator{
    /*
    def configure(context: BundleContext, system: ActorSystem) {
    	println("Starting System")
    	//val restManager = system.actorOf(Props[Rest], "restManager")
    	println("Started Actor System")
    }
    */
    
    def start(context: BundleContext) {
    	println("Starting System")
    	println(1)
    	val cl = ActorSystem.getClass.getClassLoader
    	val system = ActorSystem("second", com.typesafe.config.ConfigFactory.load(cl), cl)

    	//val system = ActorSystem("features-api")
    	println(2)
    	val restManager = system.actorOf(Props[Rest], "restManager")
    	println(3)
    	println("Started Actor System")
    }
    def stop(context: BundleContext) {
    }
}