/*
 * Copyright Inocybe Technlogies, 2014.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.rest

import org.apache.karaf.features.FeaturesService
import spray.can.Http
import akka.io.IO
import akka.util.Timeout
import akka.actor.{Actor, Props}
import akka.pattern.{ask, AskSupport }
import spray.http._
import spray.http.HttpMethods._
import spray.http.MediaTypes._
import spray.json._
import scala.concurrent.duration._
import akka.contrib.pattern.DistributedPubSubMediator
import akka.contrib.pattern.DistributedPubSubExtension
import DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import scala.util.Success
import scala.util.Failure
import scala.util.Try
import akka.actor.ActorRef


class Rest extends Actor {
    var featuresService : FeaturesService = null
    implicit val system = context.system
    IO(Http) ! Http.Bind(self, interface = "localhost", port = 8080)

    // Dependency injection
    def setFeaturesService(featuresService : FeaturesService) : Unit = {
            this.featuresService = featuresService
    }

    def receive = {
        case _: Http.Connected =>
         sender ! Http.Register(self)
         // List features 
        case _ =>     
    }
}