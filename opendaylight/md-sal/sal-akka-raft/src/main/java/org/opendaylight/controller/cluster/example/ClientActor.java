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
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import org.opendaylight.controller.cluster.example.messages.KeyValue;
import org.opendaylight.controller.cluster.example.messages.KeyValueSaved;

public class ClientActor extends UntypedActor {
    protected final LoggingAdapter LOG =
        Logging.getLogger(getContext().system(), this);

    private final ActorRef target;

    public ClientActor(ActorRef target){
        this.target = target;
    }

    public static Props props(final ActorRef target){
        return Props.create(new Creator<ClientActor>(){

            @Override public ClientActor create() throws Exception {
                return new ClientActor(target);
            }
        });
    }

    @Override public void onReceive(Object message) throws Exception {
        if(message instanceof KeyValue) {
            target.tell(message, getSelf());
        } else if(message instanceof KeyValueSaved){
            LOG.info("KeyValue saved");
        }
    }
}
