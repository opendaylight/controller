/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;
import org.apache.pekko.actor.Props;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;

public class ForwardMessageToBehaviorActor extends MessageCollectorActor {
    private final List<RaftActorBehavior> behaviorChanges = new ArrayList<>();

    private volatile RaftActorBehavior behavior;

    @Override
    public void onReceive(final Object message) throws Exception {
        if (behavior != null) {
            behaviorChanges.add(behavior.handleMessage(getSender(), message));
        }
        super.onReceive(message);
    }

    public static Props props() {
        return Props.create(ForwardMessageToBehaviorActor.class);
    }

    public void setBehavior(final RaftActorBehavior behavior) {
        this.behavior = behavior;
    }

    public RaftActorBehavior getFirstBehaviorChange() {
        assertFalse("no behavior changes present", behaviorChanges.isEmpty());
        return behaviorChanges.getFirst();
    }

    public RaftActorBehavior getLastBehaviorChange() {
        assertFalse("no behavior changes present", behaviorChanges.isEmpty());
        return behaviorChanges.getLast();
    }

    public List<RaftActorBehavior> getBehaviorChanges() {
        return behaviorChanges;
    }

    public void clear() {
        clearMessages(self());
        behaviorChanges.clear();
    }
}
