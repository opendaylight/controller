/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSystem;
import akka.actor.Props;
import com.google.common.base.Function;
import com.typesafe.config.ConfigFactory;

import javax.annotation.Nullable;

public class ActorSystemFactory {
    private static final ActorSystem actorSystem = (new Function<Void, ActorSystem>(){

        @Nullable @Override public ActorSystem apply(@Nullable Void aVoid) {
                ActorSystem system =
                    ActorSystem.create("opendaylight-cluster", ConfigFactory
                        .load().getConfig("ODLCluster"));
                system.actorOf(Props.create(TerminationMonitor.class), "termination-monitor");
                return system;
        }
    }).apply(null);

    public static final ActorSystem getInstance(){
        return actorSystem;
    }
}
