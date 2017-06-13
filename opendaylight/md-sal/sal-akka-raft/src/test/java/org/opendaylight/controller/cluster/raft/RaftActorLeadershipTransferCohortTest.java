/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import akka.dispatch.Dispatchers;
import com.google.common.base.Function;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.RaftActorLeadershipTransferCohort.OnComplete;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;

/**
 * Unit tests for RaftActorLeadershipTransferCohort.
 *
 * @author Thomas Pantelis
 */
public class RaftActorLeadershipTransferCohortTest extends AbstractActorTest {
    private final TestActorFactory factory = new TestActorFactory(getSystem());
    private MockRaftActor mockRaftActor;
    private RaftActorLeadershipTransferCohort cohort;
    private final OnComplete onComplete = mock(OnComplete.class);
    private final DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
    private Function<Runnable, Void> pauseLeaderFunction;

    @After
    public void tearDown() {
        factory.close();
    }

    private void setup(String testName) {
        String persistenceId = factory.generateActorId(testName + "-leader-");
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());
        mockRaftActor = factory.<MockRaftActor>createTestActor(MockRaftActor.builder().id(persistenceId).config(config)
                .pauseLeaderFunction(pauseLeaderFunction).props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                persistenceId).underlyingActor();
        cohort = new RaftActorLeadershipTransferCohort(mockRaftActor);
        cohort.addOnComplete(onComplete);
        mockRaftActor.waitForInitializeBehaviorComplete();
    }

    @Test
    public void testOnNewLeader() {
        setup("testOnNewLeader");
        cohort.setNewLeaderTimeoutInMillis(20000);

        cohort.onNewLeader("new-leader");
        verify(onComplete, never()).onSuccess(mockRaftActor.self());

        cohort.transferComplete();

        cohort.onNewLeader(null);
        verify(onComplete, never()).onSuccess(mockRaftActor.self());

        cohort.onNewLeader("new-leader");
        verify(onComplete).onSuccess(mockRaftActor.self());
    }

    @Test
    public void testNewLeaderTimeout() {
        setup("testNewLeaderTimeout");
        cohort.setNewLeaderTimeoutInMillis(200);
        cohort.transferComplete();
        verify(onComplete, timeout(3000)).onSuccess(mockRaftActor.self());
    }

    @Test
    public void testNotLeaderOnDoTransfer() {
        setup("testNotLeaderOnDoTransfer");
        cohort.doTransfer();
        verify(onComplete).onSuccess(mockRaftActor.self());
    }

    @Test
    public void testAbortTransfer() {
        setup("testAbortTransfer");
        cohort.abortTransfer();
        verify(onComplete).onFailure(mockRaftActor.self());
    }

    @Test
    public void testPauseLeaderTimeout() {
        pauseLeaderFunction = input -> null;
        setup("testPauseLeaderTimeout");

        Leader leader = new Leader(mockRaftActor.getRaftActorContext()) {
            @Override
            public void transferLeadership(RaftActorLeadershipTransferCohort leadershipTransferCohort) {
                leadershipTransferCohort.transferComplete();
            }
        };
        mockRaftActor.setCurrentBehavior(leader);

        cohort.init();
        verify(onComplete, timeout(2000)).onSuccess(mockRaftActor.self());
    }
}
