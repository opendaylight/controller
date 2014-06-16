package org.akka.essentials.remotenode;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.kernel.Bootable;

import com.typesafe.config.ConfigFactory;

public class RemoteNodeApplication implements Bootable {

	final ActorSystem system = ActorSystem.create("RemoteNodeApp", ConfigFactory
			.load().getConfig("RemoteSys"));


	public void shutdown() {
		system.shutdown();
	}

	public void startup() {
    System.out.println(ConfigFactory
        .load().getConfig("RemoteSys.akka"));
		system.actorOf(Props.create(org.akka.essentials.remotenode.RemoteActor.class), "remoteActor");
	}
}
