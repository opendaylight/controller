/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.utils;

import akka.actor.Props;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractRaftActorBehavior;

public class ForwardMessageToBehaviorActor extends MessageCollectorActor {
    AbstractRaftActorBehavior behavior;

    @Override public void onReceive(Object message) throws Exception {
        if(behavior != null) {
            behavior.handleMessage(sender(), message);
        }

        super.onReceive(message);
    }

    public static Props props() {
        return Props.create(ForwardMessageToBehaviorActor.class);
    }

    public void setBehavior(AbstractRaftActorBehavior behavior){
        this.behavior = behavior;
    }
}

