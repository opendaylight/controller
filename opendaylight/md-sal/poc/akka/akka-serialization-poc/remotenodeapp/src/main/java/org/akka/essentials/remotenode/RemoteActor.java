package org.akka.essentials.remotenode;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;


import org.opendaylight.controller.mdsal.Messages.PostOffice;
import org.opendaylight.controller.mdsal.Messages.PostBox;
import org.opendaylight.controller.mdsal.Messages.PostBox.Mail;

public class RemoteActor extends UntypedActor {
	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof String) {
			// Get reference to the message sender and reply back
			getSender().tell(message + " got something", ActorRef.noSender());
		}else if (message instanceof PostOffice){
           PostOffice po = (PostOffice)message;
        System.out.println("Received a logical post office message ");
        System.out.println("PostOffice name:"+po.getBox(0).getName());
        System.out.println("PostOffice id:"+po.getBox(0).getId());
      getSender().tell( "PostOffice Object with version "+po.getVersion() +"received!", ActorRef.noSender());
    }
	}
}
