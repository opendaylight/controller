/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import akka.actor.UntypedActor;

import java.util.ArrayList;
import java.util.List;

/**
 * MessageCollectorActor collects messages as it receives them. It can send
 * those collected messages to any sender which sends it the "messages" message
 * <p>
 *     This class would be useful as a mock to test whether messages were sent
 *     to a remote actor or not.
 * </p>
 */
public class MessageCollectorActor extends UntypedActor {
    private List<Object> messages = new ArrayList<>();

    @Override public void onReceive(Object message) throws Exception {
        if(message instanceof String){
            if("messages".equals(message)){
                getSender().tell(messages, getSelf());
            }
        } else {
            messages.add(message);
        }
    }
}
