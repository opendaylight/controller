/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActor.Abort;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActor.CanCommit;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActor.Commit;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActor.CommitProtocolCommand;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActor.PreCommit;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActor.Success;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.mdsal.common.api.PostCanCommitStep;
import org.opendaylight.mdsal.common.api.PostPreCommitStep;
import org.opendaylight.mdsal.common.api.ThreePhaseCommitStep;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCandidate;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataValidationFailedException;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import scala.concurrent.Await;

/**
 * Unit tests for DataTreeCohortActor.
 *
 * @author Thomas Pantelis
 */
public class DataTreeCohortActorTest extends AbstractActorTest {
    private static final Collection<DOMDataTreeCandidate> CANDIDATES = new ArrayList<>();
    private static final EffectiveModelContext MOCK_SCHEMA = mock(EffectiveModelContext.class);
    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());
    private final DOMDataTreeCommitCohort mockCohort = mock(DOMDataTreeCommitCohort.class);
    private final PostCanCommitStep mockPostCanCommit = mock(PostCanCommitStep.class);
    private final PostPreCommitStep mockPostPreCommit = mock(PostPreCommitStep.class);

    @Before
    public void setup() {
        resetMockCohort();
    }

    @After
    public void tearDown() {
        actorFactory.close();
    }

    @Test
    public void testSuccessfulThreePhaseCommit() throws Exception {
        ActorRef cohortActor = newCohortActor("testSuccessfulThreePhaseCommit");

        TransactionIdentifier txId = nextTransactionId();
        askAndAwait(cohortActor, new CanCommit(txId, CANDIDATES, MOCK_SCHEMA, cohortActor));
        verify(mockCohort).canCommit(txId, MOCK_SCHEMA, CANDIDATES);

        askAndAwait(cohortActor, new PreCommit(txId));
        verify(mockPostCanCommit).preCommit();

        askAndAwait(cohortActor, new Commit(txId));
        verify(mockPostPreCommit).commit();

        resetMockCohort();
        askAndAwait(cohortActor, new CanCommit(txId, CANDIDATES, MOCK_SCHEMA, cohortActor));
        verify(mockCohort).canCommit(txId, MOCK_SCHEMA, CANDIDATES);
    }

    @Test
    public void testMultipleThreePhaseCommits() throws Exception {
        ActorRef cohortActor = newCohortActor("testMultipleThreePhaseCommits");

        TransactionIdentifier txId1 = nextTransactionId();
        TransactionIdentifier txId2 = nextTransactionId();

        askAndAwait(cohortActor, new CanCommit(txId1, CANDIDATES, MOCK_SCHEMA, cohortActor));
        askAndAwait(cohortActor, new CanCommit(txId2, CANDIDATES, MOCK_SCHEMA, cohortActor));

        askAndAwait(cohortActor, new PreCommit(txId1));
        askAndAwait(cohortActor, new PreCommit(txId2));

        askAndAwait(cohortActor, new Commit(txId1));
        askAndAwait(cohortActor, new Commit(txId2));
    }

    @Test
    public void testAsyncCohort() throws Exception {
        final var executor = Executors.newSingleThreadExecutor();

        doReturn(executeWithDelay(executor, mockPostCanCommit))
                .when(mockCohort).canCommit(any(Object.class), any(EffectiveModelContext.class), anyCollection());

        doReturn(Futures.submit(() -> mockPostPreCommit, executor)).when(mockPostCanCommit).preCommit();

        doReturn(Futures.submit(() -> null, executor)).when(mockPostPreCommit).commit();

        ActorRef cohortActor = newCohortActor("testAsyncCohort");

        TransactionIdentifier txId = nextTransactionId();
        askAndAwait(cohortActor, new CanCommit(txId, CANDIDATES, MOCK_SCHEMA, cohortActor));
        verify(mockCohort).canCommit(txId, MOCK_SCHEMA, CANDIDATES);

        askAndAwait(cohortActor, new PreCommit(txId));
        verify(mockPostCanCommit).preCommit();

        askAndAwait(cohortActor, new Commit(txId));
        verify(mockPostPreCommit).commit();

        executor.shutdownNow();
    }

    @Test
    public void testFailureOnCanCommit() throws Exception {
        DataValidationFailedException failure = new DataValidationFailedException(YangInstanceIdentifier.of(),
                "mock");
        doReturn(FluentFutures.immediateFailedFluentFuture(failure)).when(mockCohort).canCommit(any(Object.class),
                any(EffectiveModelContext.class), anyCollection());

        ActorRef cohortActor = newCohortActor("testFailureOnCanCommit");

        TransactionIdentifier txId = nextTransactionId();
        try {
            askAndAwait(cohortActor, new CanCommit(txId, CANDIDATES, MOCK_SCHEMA, cohortActor));
        } catch (DataValidationFailedException e) {
            assertEquals("DataValidationFailedException", failure, e);
        }

        resetMockCohort();
        askAndAwait(cohortActor, new CanCommit(txId, CANDIDATES, MOCK_SCHEMA, cohortActor));
        verify(mockCohort).canCommit(txId, MOCK_SCHEMA, CANDIDATES);
    }

    @Test
    public void testAbortAfterCanCommit() throws Exception {
        ActorRef cohortActor = newCohortActor("testAbortAfterCanCommit");

        TransactionIdentifier txId = nextTransactionId();
        askAndAwait(cohortActor, new CanCommit(txId, CANDIDATES, MOCK_SCHEMA, cohortActor));
        verify(mockCohort).canCommit(txId, MOCK_SCHEMA, CANDIDATES);

        askAndAwait(cohortActor, new Abort(txId));
        verify(mockPostCanCommit).abort();

        resetMockCohort();
        askAndAwait(cohortActor, new CanCommit(txId, CANDIDATES, MOCK_SCHEMA, cohortActor));
        verify(mockCohort).canCommit(txId, MOCK_SCHEMA, CANDIDATES);
    }

    @Test
    public void testAbortAfterPreCommit() throws Exception {
        ActorRef cohortActor = newCohortActor("testAbortAfterPreCommit");

        TransactionIdentifier txId = nextTransactionId();
        askAndAwait(cohortActor, new CanCommit(txId, CANDIDATES, MOCK_SCHEMA, cohortActor));
        verify(mockCohort).canCommit(txId, MOCK_SCHEMA, CANDIDATES);

        askAndAwait(cohortActor, new PreCommit(txId));
        verify(mockPostCanCommit).preCommit();

        askAndAwait(cohortActor, new Abort(txId));
        verify(mockPostPreCommit).abort();
    }

    private static <T> FluentFuture<T> executeWithDelay(final Executor executor, final T result) {
        return FluentFutures.submit(() -> {
            Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
            return result;
        }, executor);
    }

    private ActorRef newCohortActor(final String name) {
        return actorFactory.createActor(DataTreeCohortActor.props(name, mockCohort, YangInstanceIdentifier.of()), name);
    }

    private void resetMockCohort() {
        reset(mockCohort);
        doReturn(ThreePhaseCommitStep.NOOP_ABORT_FUTURE).when(mockPostCanCommit).abort();
        doReturn(Futures.immediateFuture(mockPostPreCommit)).when(mockPostCanCommit).preCommit();
        doReturn(FluentFutures.immediateFluentFuture(mockPostCanCommit)).when(mockCohort).canCommit(any(Object.class),
                any(EffectiveModelContext.class), anyCollection());

        doReturn(ThreePhaseCommitStep.NOOP_ABORT_FUTURE).when(mockPostPreCommit).abort();
        doReturn(Futures.immediateFuture(null)).when(mockPostPreCommit).commit();
    }

    private static void askAndAwait(final ActorRef actor, final CommitProtocolCommand<?> message) throws Exception {
        Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
        Object result = Await.result(Patterns.ask(actor, message, timeout), timeout.duration());
        assertTrue("Expected Success but was " + result, result instanceof Success);
        assertEquals("Success", message.getTxId(), ((Success)result).getTxId());
    }
}
