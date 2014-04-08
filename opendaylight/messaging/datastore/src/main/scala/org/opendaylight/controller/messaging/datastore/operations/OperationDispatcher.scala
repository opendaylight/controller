/*
 * Copyright Inocybe Technlogies, 2014.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messaging.datastore.operations

import akka.io.IO
import akka.util.Timeout
import akka.pattern.{ask, AskSupport }
import akka.actor.{ Actor, ActorLogging }
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import spray.can.Http
import spray.json._
import spray.http._
import spray.http.MediaTypes._
import spray.http.HttpMethods._
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import scala.util.{Success, Failure}
import ca.inocybe.cerebrum._
import scala.util.Try
import akka.actor.ActorRef
import akka.dispatch.OnComplete

trait OperationInstructions
object Nodes extends OperationInstructions

class OperationsProcessor extends Actor with ActorLogging{

  def actorRefFactory = context

  import context.dispatcher
  import context.system
  
  implicit val timeout = Timeout(5 seconds)

  // Creates the Http tunnel, gets appropriate settings from config file, and binds this actor to the tunnel.
  val config = ConfigFactory.load()
  val webService = IO(Http)
  val webServiceInterface = config.getString("cortex.auditory.interface")
  val webServicePort = config.getInt("cortex.auditory.port")
  webService ! Http.Bind(self, interface = webServiceInterface, port = webServicePort)

  val mediator = DistributedPubSubExtension(context.system).mediator
        
  def receive = {
    case _: Http.Connected =>
      sender ! Http.Register(self)
      
    // Returns the list of nodes in the cluster
    case HttpRequest(GET, Uri.Path("/nodes"), _, _, _) =>
      val ori = sender
      val futures = mediator.ask(Publish("Info", Nodes)).mapTo[NorthboundReply].onComplete(reply(ori))
      
    // Returns all the data in the following form:
    // [{key: "...", value: "...", clock: "..."}, {...}]
    case HttpRequest(GET, Uri.Path("/data"), _, _, _) => 
      val ori = sender
      val futures = mediator.ask(Publish("Info", Data)).mapTo[NorthboundReply].onComplete(reply(ori))
      
    // Store the K/V pair in the DB.
    case HttpRequest(PUT, Uri.Path("/memorize"), _, entity, _) =>
      val ori = sender
      val map = entity.asString.split("&").map(_.split("=")).map(x => if(x.length > 1) (x(0), x(1)) else (x(0), "")).toMap
      mediator.ask(Publish("Router", Memorize(map("key"), map("value")))).mapTo[NorthboundReply].onComplete(reply(ori))
      
    // Updates the K/V pair.
    case HttpRequest(PUT, Uri.Path("/reinforce"), _, entity, _) =>
      val ori = sender
      val map = entity.asString.split("&").map(_.split("=")).map(x => if(x.length > 1) (x(0), x(1)) else (x(0), "")).toMap
      mediator.ask(Publish("Router", Reinforce(map("key"), map("value")))).mapTo[NorthboundReply].onComplete(reply(ori))
      
    // Get an object with Key x.
    case HttpRequest(GET, path @ Uri.Path("/recall"), _, _, _) =>
      val ori = sender
      val key = path.query.get("key").get
      mediator.ask(Publish("Router", Recall(key))).mapTo[NorthboundReply].onComplete(reply(ori))
      
    // Removes an object with Key x from the DB.
    case HttpRequest(DELETE, path @ Uri.Path("/forget"), _, _, _) =>
      val ori = sender
      val key = path.query.get("key").get
      mediator.ask(Publish("Router", Forget(key))).mapTo[NorthboundReply].onComplete(reply(ori))
  }

  def reply(a: ActorRef): PartialFunction[Try[NorthboundReply], Unit] = {
    case Success(OperationOK(message)) => message.isDefined match {
      case true 	=> a ! HttpResponse(entity = HttpEntity(`application/json`, message.get.toString))
      case false 	=> a ! HttpResponse(status = StatusCodes.OK)
    }
    case Success(OperationNotFound()) 	=> a ! HttpResponse(status = StatusCodes.NotFound)
    case Success(OperationFailed()) 	=> a ! HttpResponse(status = StatusCodes.InternalServerError)
    case m								=> 
      println(m)
      a ! HttpResponse(status = StatusCodes.InternalServerError)
  }
}