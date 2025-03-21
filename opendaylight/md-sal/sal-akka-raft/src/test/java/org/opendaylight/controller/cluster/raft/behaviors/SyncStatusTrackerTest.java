/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.apache.pekko.actor.ActorRef;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.AbstractActorTest;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.base.messages.FollowerInitialSyncUpStatus;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;

public class SyncStatusTrackerTest extends AbstractActorTest {
    protected final TestActorFactory actorFactory = new TestActorFactory(getSystem());

    private final ActorRef listener = actorFactory.createActor(
            MessageCollectorActor.props(), actorFactory.generateActorId("listener"));

    @After
    public void tearDown() {
        actorFactory.close();
    }

    @Test
    public void testUpdate() {
        SyncStatusTracker tracker = new SyncStatusTracker(listener, "commit-tracker", 10);

        // When leader-1 sends the first update message the listener should receive a syncStatus notification
        // with status set to false
        tracker.update("leader-1", 100, 99);
        FollowerInitialSyncUpStatus status =
                MessageCollectorActor.getFirstMatching(listener, FollowerInitialSyncUpStatus.class);

        assertFalse(status.initialSyncDone());
        MessageCollectorActor.clearMessages(listener);

        // At a minimum the follower should have the commit index that the new leader sent it in the first message
        // Also the commit index must be below the syncThreshold. If both conditions are met a new sync status
        // message with status = true should be expected
        tracker.update("leader-1", 105, 101);

        status = MessageCollectorActor.getFirstMatching(listener, FollowerInitialSyncUpStatus.class);

        assertTrue(status.initialSyncDone());
        MessageCollectorActor.clearMessages(listener);

        // If a subsequent message is received and if the difference between the followers commit index and
        // the leaders commit index is below the syncThreshold then no status notification must be issues
        tracker.update("leader-1", 108, 101);

        status = MessageCollectorActor.getFirstMatching(listener, FollowerInitialSyncUpStatus.class);

        assertNull("No status message should be received", status);

        // If the follower falls behind the leader by more than the syncThreshold then the listener should
        // receive a syncStatus notification with status = false
        tracker.update("leader-1", 150, 101);

        status = MessageCollectorActor.getFirstMatching(listener, FollowerInitialSyncUpStatus.class);

        assertNotNull("No sync status message was received", status);

        assertFalse(status.initialSyncDone());
        MessageCollectorActor.clearMessages(listener);

        // If the follower is not caught up yet it should not receive any further notification
        tracker.update("leader-1", 150, 125);

        status = MessageCollectorActor.getFirstMatching(listener, FollowerInitialSyncUpStatus.class);

        assertNull("No status message should be received", status);

        // Once the syncThreshold is met a new syncStatus notification should be issued
        tracker.update("leader-1", 160, 155);

        status = MessageCollectorActor.getFirstMatching(listener, FollowerInitialSyncUpStatus.class);

        assertTrue(status.initialSyncDone());
        MessageCollectorActor.clearMessages(listener);

        // When a new leader starts sending update messages a new syncStatus notification should be immediately
        // triggered with status = false
        tracker.update("leader-2", 160, 155);

        status = MessageCollectorActor.getFirstMatching(listener, FollowerInitialSyncUpStatus.class);

        assertFalse(status.initialSyncDone());
        MessageCollectorActor.clearMessages(listener);

        // If an update is received from a new leader which is still below the minimum expected index then
        // syncStatus should not be changed
        tracker.update("leader-2", 160, 159);

        status = MessageCollectorActor.getFirstMatching(listener, FollowerInitialSyncUpStatus.class);

        assertNull("No status message should be received", status);
    }

    @Test
    public void testConstructorActorShouldNotBeNull() {
        final var ex = assertThrows(NullPointerException.class, () -> new SyncStatusTracker(null, "commit-tracker", 1));
        assertEquals("actor", ex.getMessage());
    }

    @Test
    public void testConstructorIdShouldNotBeNull() {
        final var ex = assertThrows(NullPointerException.class, () -> new SyncStatusTracker(listener, null, 1));
        assertEquals("memberId should not be null", ex.getMessage());
    }

    @Test
    public void testConstructorSyncThresholdShouldNotBeNegative() {
        final var ex = assertThrows(IllegalArgumentException.class,
            () -> new SyncStatusTracker(listener, "commit-tracker", -1));
        assertEquals("syncThreshold should be greater than or equal to 0", ex.getMessage());
    }
}
