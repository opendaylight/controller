/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;

/**
 * Tests end-to-end replication of sliced log entry payloads, ie entries whose size exceeds the maximum size for a
 * single AppendEntries message.
 *
 * @author Thomas Pantelis
 */
class ReplicationWithSlicedPayloadIntegrationTest extends AbstractRaftActorIntegrationTest {
    @Test
    void runTest() {
        testLog.info("ReplicationWithSlicedPayloadIntegrationTest starting");

        // Create the leader and 2 follower actors.

        maximumMessageSliceSize = 20;

        DefaultConfigParamsImpl followerConfigParams = newFollowerConfigParams();
        followerConfigParams.setSnapshotBatchCount(snapshotBatchCount);
        follower1Actor = newTestRaftActor(follower1Id, Map.of(leaderId, testActorPath(leaderId),
                follower2Id, testActorPath(follower2Id)), followerConfigParams);

        follower2Actor = newTestRaftActor(follower2Id, Map.of(leaderId, testActorPath(leaderId),
                follower1Id, testActorPath(follower1Id)), followerConfigParams);

        peerAddresses = Map.of(
                follower1Id, follower1Actor.path().toString(),
                follower2Id, follower2Actor.path().toString());

        leaderConfigParams = newLeaderConfigParams();
        leaderActor = newTestRaftActor(leaderId, peerAddresses, leaderConfigParams);

        follower1Collector = follower1Actor.underlyingActor().collector();
        follower2Collector = follower2Actor.underlyingActor().collector();
        leaderCollector = leaderActor.underlyingActor().collector();

        leaderContext = leaderActor.underlyingActor().getRaftActorContext();

        waitUntilLeader(leaderActor);

        currentTerm = leaderContext.currentTerm();

        // Send a large payload that exceeds the size threshold and needs to be sliced.

        MockCommand largePayload = sendPayloadData(leaderActor, "large", maximumMessageSliceSize + 1);

        // Then send a small payload that does not need to be sliced.

        MockCommand smallPayload = sendPayloadData(leaderActor, "normal", maximumMessageSliceSize - 1);

        final var leaderApplyState = leaderCollector.expectMatching(ApplyState.class, 2);
        verifyApplyState(leaderApplyState.get(0), leaderCollector.actor(),
                largePayload.toString(), currentTerm, 0, largePayload);
        verifyApplyState(leaderApplyState.get(1), leaderCollector.actor(),
                smallPayload.toString(), currentTerm, 1, smallPayload);

        final var follower1ApplyState = follower1Collector.expectMatching(ApplyState.class, 2);
        verifyApplyState(follower1ApplyState.get(0), null, null, currentTerm, 0, largePayload);
        verifyApplyState(follower1ApplyState.get(1), null, null, currentTerm, 1, smallPayload);

        final var follower2ApplyState = follower2Collector.expectMatching(ApplyState.class, 2);
        verifyApplyState(follower2ApplyState.get(0), null, null, currentTerm, 0, largePayload);
        verifyApplyState(follower2ApplyState.get(1), null, null, currentTerm, 1, smallPayload);

        testLog.info("ReplicationWithSlicedPayloadIntegrationTest ending");
    }
}
