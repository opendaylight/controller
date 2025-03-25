/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.expectMatching;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;

/**
 * Tests end-to-end replication of sliced log entry payloads, ie entries whose size exceeds the maximum size for a
 * single AppendEntries message.
 *
 * @author Thomas Pantelis
 */
public class ReplicationWithSlicedPayloadIntegrationTest extends AbstractRaftActorIntegrationTest {

    @Test
    public void runTest() {
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

        follower1CollectorActor = follower1Actor.underlyingActor().collectorActor();
        follower2CollectorActor = follower2Actor.underlyingActor().collectorActor();
        leaderCollectorActor = leaderActor.underlyingActor().collectorActor();

        leaderContext = leaderActor.underlyingActor().getRaftActorContext();

        waitUntilLeader(leaderActor);

        currentTerm = leaderContext.currentTerm();

        // Send a large payload that exceeds the size threshold and needs to be sliced.

        MockPayload largePayload = sendPayloadData(leaderActor, "large", maximumMessageSliceSize + 1);

        // Then send a small payload that does not need to be sliced.

        MockPayload smallPayload = sendPayloadData(leaderActor, "normal", maximumMessageSliceSize - 1);

        final List<ApplyState> leaderApplyState = expectMatching(leaderCollectorActor, ApplyState.class, 2);
        verifyApplyState(leaderApplyState.get(0), leaderCollectorActor,
                largePayload.toString(), currentTerm, 0, largePayload);
        verifyApplyState(leaderApplyState.get(1), leaderCollectorActor,
                smallPayload.toString(), currentTerm, 1, smallPayload);

        final List<ApplyState> follower1ApplyState = expectMatching(follower1CollectorActor, ApplyState.class, 2);
        verifyApplyState(follower1ApplyState.get(0), null, null, currentTerm, 0, largePayload);
        verifyApplyState(follower1ApplyState.get(1), null, null, currentTerm, 1, smallPayload);

        final List<ApplyState> follower2ApplyState = expectMatching(follower2CollectorActor, ApplyState.class, 2);
        verifyApplyState(follower2ApplyState.get(0), null, null, currentTerm, 0, largePayload);
        verifyApplyState(follower2ApplyState.get(1), null, null, currentTerm, 1, smallPayload);

        testLog.info("ReplicationWithSlicedPayloadIntegrationTest ending");
    }
}
