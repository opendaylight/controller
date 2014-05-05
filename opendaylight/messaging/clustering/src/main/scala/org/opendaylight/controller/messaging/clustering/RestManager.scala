/*
 * Copyright Inocybe Technlogies, 2014.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messaging.clustering

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

class RestManager extends Actor{
  
  def actorRefFactory = context
  implicit val timeout = Timeout(5 seconds)
  
  import context.dispatcher
  import context.system
   
  IO(Http) ! Http.Bind(self, interface = "localhost", port = 8080)
     	  		  						       
  val mediator = DistributedPubSubExtension(context.system).mediator
  
  def receive = {
    case _: Http.Connected =>
      sender ! Http.Register(self)
      
    case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
      sender ! HttpResponse(status = StatusCodes.OK)
      
    case HttpRequest(GET, Uri.Path("/allnodes"), _, _, _) =>
      val original = sender
      mediator.ask(Publish("clusterListener", "allnodes")).onComplete{completeCont(original)}
     
    case HttpRequest(GET, path @ Uri.Path("/node/metrics"), _, _, _) =>
      val original = sender
      val id = path.query.get("id")
      if(id.isDefined){
        mediator.ask(Publish("clusterListener", ("getMetrics", id.get))).onComplete{completeCont(original)}
      }else{
        original ! HttpResponse(status = StatusCodes.BadRequest)
      }
      
    case HttpRequest(POST, path @ Uri.Path("/node/leave"), _, _, _) =>
      val original = sender
      val id = path.query.get("id")
      if(id.isDefined){
        mediator.ask(Publish("clusterListener", ("leaveCluster", id.get))).onComplete{completeCont(original)}
      }else{
        original ! HttpResponse(status = StatusCodes.BadRequest)
      }
        
    case _ => 
  }   
  
  def okResponse(content: JsValue) = HttpResponse(
		  						       entity = HttpEntity(`application/json`, content.toString), 
		  						       status = StatusCodes.OK)
		  						       
  def completeCont(sender: ActorRef): PartialFunction[Try[Any], Unit] = {
    case Success(message: JsValue) => sender ! okResponse(message)
    case Success(_) | Failure(_)   => sender ! HttpResponse(status = StatusCodes.InternalServerError)
  }
}