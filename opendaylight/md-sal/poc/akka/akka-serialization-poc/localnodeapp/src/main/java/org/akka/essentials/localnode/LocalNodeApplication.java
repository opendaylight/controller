package org.akka.essentials.localnode;

import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import org.opendaylight.controller.mdsal.Messages.PostOffice;
import org.opendaylight.controller.mdsal.Messages.PostBox;
import org.opendaylight.controller.mdsal.Messages.PostBox.Mail;


/**
 * Hello world!
 * 
 */
public class LocalNodeApplication {
	public static void main(String[] args) throws Exception {
		ActorSystem _system = ActorSystem.create("LocalNodeApp",ConfigFactory
				.load().getConfig("LocalSys"));
		ActorRef localActor = _system.actorOf(Props.create(LocalActor.class));
		localActor.tell("Hello",ActorRef.noSender());

      //this is a protocol buffers message
      PostBox.Builder postBoxBuilder = PostBox.newBuilder();
      PostBox postBox = postBoxBuilder.addMail(Mail.newBuilder().setType(PostBox.FromType.LOCAL)
          .setZipcode("95050").build()).setId("1").setName("Test Post Box")
          .build();
      PostOffice  postOffice =    PostOffice.newBuilder().addBox(postBox).setVersion("1.0").build();

      localActor.tell(postOffice,ActorRef.noSender());
    Thread.sleep(60000);
    _system.shutdown();

	}
}
