package org.akka.essentials.localnode;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import org.opendaylight.controller.mdsal.Messages.PostOffice;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;


public class LocalActor extends UntypedActor {
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);


	ActorRef remoteActor;

	@Override
	public void preStart() {
		//Get a reference to the remote actor
		remoteActor = getContext().actorFor(
				"akka.tcp://RemoteNodeApp@127.0.0.1:2552/user/remoteActor");
	}

	@Override
	public void onReceive(Object message) throws Exception {
   if(message instanceof String) {
		Future<Object> future = Patterns.ask(remoteActor, message.toString(),			5000);
		String result = (String) Await.result(future,Duration.create(5, "seconds") );
     log.info("Message received from Server -> {}", result);
   }else if(message instanceof PostOffice){
     Future<Object> future = Patterns.ask(remoteActor, message, 5000);
     String result = (String) Await.result(future, Duration.create(5000, "seconds"));
     log.info("Message received from Server for Post Office message-> {}", result);

   }
	}
}
