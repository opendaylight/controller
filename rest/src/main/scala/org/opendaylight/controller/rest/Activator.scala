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


class Activator extends BundleActivator{
    def stop(context: BundleContext) {
    }
    def start(context: BundleContext) {
    	println("Starting System")
    	val system = ActorSystem("features-api")
    	val restManager = system.actorOf(Props[Rest], "restManager")
    }
}