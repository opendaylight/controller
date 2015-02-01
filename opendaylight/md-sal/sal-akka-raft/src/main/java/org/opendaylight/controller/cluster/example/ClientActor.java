/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.example;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import org.opendaylight.controller.cluster.example.messages.KeyValue;
import org.opendaylight.controller.cluster.example.messages.KeyValueSaved;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientActor extends UntypedActor {
    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    private final ActorRef target;

    public ClientActor(ActorRef target){
        this.target = target;
    }

    public static Props props(final ActorRef target) {
        return Props.create(ClientActor.class, target);
    }

    @Override public void onReceive(Object message) throws Exception {
        if(message instanceof KeyValue) {
            target.tell(message, getSelf());
        } else if(message instanceof KeyValueSaved){
            LOG.info("KeyValue saved");
        }
    }
}
