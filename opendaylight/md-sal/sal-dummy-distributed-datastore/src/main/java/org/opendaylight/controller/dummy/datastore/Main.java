/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.dummy.datastore;

import akka.actor.ActorSystem;
import com.typesafe.config.ConfigFactory;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class Main {
    @Option(name="-member-name", usage="Sets the member name", required = true)
    public String memberName;

    @Option(name="-max-delay-millis", usage = "Sets the maximum delay that should be applied for any append entry. Only applies when cause-trouble is present.")
    public int maxDelayInMillis = 500;

    @Option(name="-cause-trouble", usage="If present turns on artificial failures")
    public boolean causeTrouble = false;

    @Option(name="-drop-replies", usage = "If present drops replies. Only applies when cause-trouble is present.")
    public boolean dropReplies = false;

    public void run(){
        ActorSystem actorSystem = ActorSystem.create("opendaylight-cluster-data", ConfigFactory.load(memberName).getConfig("odl-cluster-data"));

        Configuration configuration = new Configuration(maxDelayInMillis, dropReplies, causeTrouble);

        actorSystem.actorOf(DummyShardManager.props(configuration, memberName, new String[] {"inventory", "default", "toaster", "topology"}, "operational"), "shardmanager-operational");
        actorSystem.actorOf(DummyShardManager.props(configuration, memberName, new String[] {"inventory", "default", "toaster", "topology"}, "config"), "shardmanager-config");
    }

    @Override
    public String toString() {
        return "Main{" +
                "memberName='" + memberName + '\'' +
                ", maxDelayInMillis=" + maxDelayInMillis +
                ", causeTrouble=" + causeTrouble +
                ", dropReplies=" + dropReplies +
                '}';
    }

    public static void main(String[] args){
        Main bean = new Main();
        CmdLineParser parser = new CmdLineParser(bean);

        try {
            parser.parseArgument(args);
            System.out.println(bean.toString());
            bean.run();
        } catch(Exception e){
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }
    }

}
