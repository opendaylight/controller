/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.slf4j.Logger;
import scala.concurrent.Future;

/**
 * Unit tests for DebugThreePhaseCommitCohort.
 *
 * @author Thomas Pantelis
 */
public class DebugThreePhaseCommitCohortTest {
    private final TransactionIdentifier transactionId = MockIdentifiers.transactionIdentifier(
        DebugThreePhaseCommitCohortTest.class, "mock");

    @Test
    public void test() {
        AbstractThreePhaseCommitCohort<?> mockDelegate = mock(AbstractThreePhaseCommitCohort.class);
        Exception failure = new Exception("mock failure");
        ListenableFuture<Object> expFailedFuture = Futures.immediateFailedFuture(failure);
        doReturn(expFailedFuture).when(mockDelegate).canCommit();
        doReturn(expFailedFuture).when(mockDelegate).preCommit();
        doReturn(expFailedFuture).when(mockDelegate).commit();

        ListenableFuture<Object> expAbortFuture = Futures.immediateFuture(null);
        doReturn(expAbortFuture).when(mockDelegate).abort();

        List<Future<Object>> expCohortFutures = new ArrayList<>();
        doReturn(expCohortFutures).when(mockDelegate).getCohortFutures();

        Throwable debugContext = new RuntimeException("mock");
        DebugThreePhaseCommitCohort cohort = new DebugThreePhaseCommitCohort(transactionId , mockDelegate , debugContext);

        Logger mockLogger = mock(Logger.class);
        cohort.setLogger(mockLogger);

        assertSame("canCommit", expFailedFuture, cohort.canCommit());
        verify(mockLogger).warn(anyString(), same(transactionId), same(failure), same(debugContext));

        reset(mockLogger);
        assertSame("preCommit", expFailedFuture, cohort.preCommit());
        verify(mockLogger).warn(anyString(), same(transactionId), same(failure), same(debugContext));

        reset(mockLogger);
        assertSame("commit", expFailedFuture, cohort.commit());
        verify(mockLogger).warn(anyString(), same(transactionId), same(failure), same(debugContext));

        assertSame("abort", expAbortFuture, cohort.abort());

        assertSame("getCohortFutures", expCohortFutures, cohort.getCohortFutures());

        reset(mockLogger);
        ListenableFuture<Boolean> expSuccessFuture = Futures.immediateFuture(Boolean.TRUE);
        doReturn(expSuccessFuture).when(mockDelegate).canCommit();

        assertSame("canCommit", expSuccessFuture, cohort.canCommit());
        verify(mockLogger, never()).warn(anyString(), any(TransactionIdentifier.class), any(Throwable.class),
                any(Throwable.class));
    }
}
