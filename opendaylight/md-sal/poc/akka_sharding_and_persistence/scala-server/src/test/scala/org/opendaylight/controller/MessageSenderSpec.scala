package org.opendaylight.controller

import org.scalatest.{Matchers, path}
import akka.actor.{Actor, ActorLogging, ActorSystem}
import com.typesafe.config.ConfigFactory
import akka.persistence.Persistent
import akka.testkit.TestActorRef

class MessageSenderSpec extends path.FunSpec with Matchers{
  val totalMessages = 10;

  describe(s"SendMessages($totalMessages)") {
    var messageCount : Int = 0;


    class MockClusteringService extends Actor with ActorLogging{
      override def receive: Receive = {
        case ShardAddressByModuleName(moduleName) => {
          log.info("Aha Shard was found")
          sender ! ShardFound(moduleName, "/user/foobar")
        }
      }
    }

    class MockShard extends Actor with ActorLogging {
      override def receive: Actor.Receive = {
        case EchoRequest(text) => {
          log.info("Received " + text)
          sender ! EchoResponse(text)
        }
        case Persistent(payload, sequenceNumber) => {
          messageCount +=1
        }
      }
    }

    it(s"should send $totalMessages Persistent messages to the remote shard")  {
      implicit val system = ActorSystem("opendaylight", ConfigFactory.load())
      val clusteringServiceActorRef = TestActorRef(new MockClusteringService);
      val shardActorRef = TestActorRef(new MockShard, "foobar");
      val actorRef = TestActorRef( new MessageSender(clusteringServiceActorRef) )
      actorRef.receive(SendMessages(totalMessages))
      Matchers.assert(messageCount == totalMessages);
    }
  }

}
