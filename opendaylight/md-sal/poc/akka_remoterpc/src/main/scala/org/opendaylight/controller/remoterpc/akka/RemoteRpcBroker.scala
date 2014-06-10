package org.opendaylight.controller.remoterpc.akka

import akka.actor.{Props, ActorSystem, Actor}
import com.typesafe.config.ConfigFactory
import akka.cluster.Cluster
import scala.concurrent.duration._
import org.opendaylight.controller.remoterpc.akka.RemoteRpcBroker.{InvokeRemoteRpc, HandleResponse, HandleRpc}

object RemoteRpcBroker {
  case class HandleRpc();
  case class InvokeRemoteRpc(rpc: String)
  case class HandleResponse()

  def main(args: Array[String]): Unit = {
    // Override the configuration of the port
    val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + args(0))
                              .withFallback(ConfigFactory.load())

    val system = ActorSystem("RemoteRpcCluster", config)
    val rpcBroker = system.actorOf(Props[RemoteRpcBroker], name = "remoterpcbroker")
    val registry = system.actorOf(Props[RouteRegistry], name = "routeregistry")

    //Register an rpc
    registry ! RouteRegistry.RegisterRpc(args(1))

    //Invoke rpc on remote member every 10 seconds.
    implicit val context = system.dispatcher
    val gossipTask = system.scheduler.schedule(10 seconds, 10 seconds, rpcBroker, InvokeRemoteRpc(args(2)))
  }
}
class RemoteRpcBroker extends Actor{



  def receive = {

    case HandleRpc =>
      println("Invoking RPC")
      sender ! HandleResponse

    case HandleResponse =>
      println("Got RPC response")

    //This is called by this actors companion object every 10 seconds
    case InvokeRemoteRpc(rpc) =>

      //Find all the members from rpc registry that can handle this rpc
      val whoHasRpc = RouteRegistry.registry.collect{
        case (address, bucket) if bucket.rpcs.contains(rpc)=> address -> bucket
      }

      //Invoking RPC on all member that have registered for this rpc.
      //In production code, however, we'll need to invoke on only one based on
      //"certain" criterion such as load balance/random/first found etc.
      if (whoHasRpc.nonEmpty){
        whoHasRpc.map {
          case (address, bucket) => {
            val path = address.toString + "/user/remoterpcbroker"
            println("remote actor path " + path)
            val remoteActor = context.actorSelection(path)
            remoteActor ! HandleRpc
          }
        }
    }

    case _ =>
      println("Do not understand")
  }

}
