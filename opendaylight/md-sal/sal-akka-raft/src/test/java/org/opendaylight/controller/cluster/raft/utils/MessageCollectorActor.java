/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.utils;

import akka.actor.UntypedActor;

import java.util.ArrayList;
import java.util.List;


public class MessageCollectorActor extends UntypedActor {
    private List<Object> messages = new ArrayList<>();

    @Override public void onReceive(Object message) throws Exception {
        if(message instanceof String){
            if("get-all-messages".equals(message)){
                getSender().tell(messages, getSelf());
            }
        } else {
            messages.add(message);
        }
    }
}
