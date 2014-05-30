/*
 * Copyright Inocybe Technlogies, 2014.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.rest

import akka.actor.Actor
import spray.http._
import spray.http.HttpMethods._
import spray.http.MediaTypes._
import spray.httpx.SprayJsonSupport._
import spray.json._
import spray.json.DefaultJsonProtocol._
import spray.routing._
import spray.routing._
import akka.actor.Props
import spray.httpx.unmarshalling.Deserialized
import akka.actor.PoisonPill

class Rest extends Actor with SimpleRoutingApp {

	import MyJsonProtocol._
    implicit val system = context.system
    val interface = "localhost"
    val port = 8080
    
	def route = {
	    pathPrefix("features") {
	    	respondWithMediaType(`application/json`) {
	        	path("list") {
	        	    get{
	        		    val json = Feature("1234", "odl-managers", "managers", "detail managers", "1.0.0-SNAPSHOT", true, "maven.org")
	        		    complete(json)
	        	    }
	        	}~
	        	path("installed") {
	        	    get{
	        		val json = Feature("1234", "odl-managers", "managers", "detail managers", "1.0.0-SNAPSHOT", true, "maven.org")
	        		val json2 = Feature("54362", "odl-cluster", "felix", "detail managers", "", false, "maven.org")
	        		val jsonList = List(json, json2)
	        		complete(jsonList)
	        	    }
	        	}
	        }~
	        path("install") {
	            post {
	                entity(as[FeatureName]){ feature =>
	                	val featureInstaller = system.actorOf(Props[FeatureManager], "featureInstaller")
	                	featureInstaller ! FeatureInstall(feature.name)
	                	featureInstaller ! PoisonPill
	                	complete("installed feature")
	                }
	            }
	        }~
	        path("uninstall") {
	            post {
	                entity(as[FeatureName]){ feature =>
	                    val featureUninstaller = system.actorOf(Props[FeatureManager], "featureUninstaller")
	                    featureUninstaller ! FeatureUninstall(feature.name)
	                    featureUninstaller ! PoisonPill
	                	complete("uninstalled feature")
	                }
	            }
	        }
	    }
	}
    startServer(interface, port, "Rest")(route)
    
    println("server running")

    def receive = {
        case _ => 
    }
    
}