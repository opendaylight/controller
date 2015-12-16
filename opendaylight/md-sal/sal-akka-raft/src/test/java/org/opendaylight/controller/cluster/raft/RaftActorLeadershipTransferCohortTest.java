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
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.RaftActorLeadershipTransferCohort.OnComplete;

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
    DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

    @After
    public void tearDown() {
        factory.close();
    }

    private void setup() {
        String persistenceId = factory.generateActorId("leader-");
        mockRaftActor = factory.<MockRaftActor>createTestActor(MockRaftActor.builder().id(persistenceId).config(
                config).props().withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId).underlyingActor();
        cohort = new RaftActorLeadershipTransferCohort(mockRaftActor, null);
        cohort.addOnComplete(onComplete);
        mockRaftActor.waitForInitializeBehaviorComplete();
    }

    @Test
    public void testOnNewLeader() {
        setup();
        cohort.setNewLeaderTimeoutInMillis(20000);

        cohort.onNewLeader("new-leader");
        verify(onComplete, never()).onSuccess(mockRaftActor.self(), null);

        cohort.transferComplete();

        cohort.onNewLeader(null);
        verify(onComplete, never()).onSuccess(mockRaftActor.self(), null);

        cohort.onNewLeader("new-leader");
        verify(onComplete).onSuccess(mockRaftActor.self(), null);
    }

    @Test
    public void testNewLeaderTimeout() {
        setup();
        cohort.setNewLeaderTimeoutInMillis(200);
        cohort.transferComplete();
        verify(onComplete, timeout(3000)).onSuccess(mockRaftActor.self(), null);
    }

    @Test
    public void testNotLeaderOnProceedWithTransfer() {
        config.setElectionTimeoutFactor(10000);
        setup();
        cohort.proceedWithTransfer();
        verify(onComplete).onSuccess(mockRaftActor.self(), null);
    }

    @Test
    public void testAbortTransfer() {
        setup();
        cohort.abortTransfer();
        verify(onComplete).onFailure(mockRaftActor.self(), null);
    }

//    static class OnCompleteCallback implements RaftActorLeadershipTransferCohort.OnComplete {
//        CountDownLatch onSuccess = new CountDownLatch(1);
//        CountDownLatch onFailure = new CountDownLatch(1);
//
//        @Override
//        public void onSuccess(ActorRef raftActorRef, ActorRef replyTo) {
//            onSuccess.countDown();
//        }
//
//        @Override
//        public void onFailure(ActorRef raftActorRef, ActorRef replyTo) {
//            onFailure.countDown();
//        }
//
//        void waitForOnSuccess() {
//            assertEquals("onSuccess invoked", true, Uninterruptibles.awaitUninterruptibly(onSuccess, 3, TimeUnit.SECONDS));
//        }
//
//        void waitForOnFailure() {
//            assertEquals("onFailure invoked", true, Uninterruptibles.awaitUninterruptibly(onFailure, 3, TimeUnit.SECONDS));
//        }
//    }
}
