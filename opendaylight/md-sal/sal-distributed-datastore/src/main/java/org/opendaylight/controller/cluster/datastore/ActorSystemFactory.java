/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSystem;
import com.typesafe.config.ConfigFactory;

public class ActorSystemFactory {
    private static final ActorSystem actorSystem =
        ActorSystem.create("opendaylight-cluster", ConfigFactory
            .load().getConfig("ODLCluster"));

    public static final ActorSystem getInstance(){
        return actorSystem;
    }
}
