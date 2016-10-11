/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.utils;

import static org.junit.Assert.assertTrue;

import akka.actor.Props;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;

public class ForwardMessageToBehaviorActor extends MessageCollectorActor {
    private volatile RaftActorBehavior behavior;
    private final List<RaftActorBehavior> behaviorChanges = new ArrayList<>();

    @Override
    public void onReceive(Object message) throws Exception {
        if (behavior != null) {
            behaviorChanges.add(behavior.handleMessage(sender(), message));
        }
        super.onReceive(message);
    }

    public static Props props() {
        return Props.create(ForwardMessageToBehaviorActor.class);
    }

    public void setBehavior(RaftActorBehavior behavior) {
        this.behavior = behavior;
    }

    public RaftActorBehavior getFirstBehaviorChange() {
        assertTrue("no behavior changes present", behaviorChanges.size() > 0);
        return behaviorChanges.get(0);
    }

    public RaftActorBehavior getLastBehaviorChange() {
        assertTrue("no behavior changes present", behaviorChanges.size() > 0);
        return behaviorChanges.get(behaviorChanges.size() - 1);
    }

    public List<RaftActorBehavior> getBehaviorChanges() {
        return behaviorChanges;
    }

    @Override
    public void clear() {
        super.clear();
        behaviorChanges.clear();
    }
}

