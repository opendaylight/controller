/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ConflictingModificationAppliedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TipProducingDataTree;

/**
 * Unit tests for SimpleShardDataTreeCohort.
 *
 * @author Thomas Pantelis
 */
public class SimpleShardDataTreeCohortTest extends AbstractTest {
    @Mock
    private TipProducingDataTree mockDataTree;

    @Mock
    private ShardDataTree mockShardDataTree;

    @Mock
    private DataTreeModification mockModification;

    @Mock
    private CompositeDataTreeCohort mockUserCohorts;

    @Mock
    private FutureCallback<DataTreeCandidate> mockPreCallback;

    @Mock
    private FutureCallback<UnsignedLong> mockCommitCallback;

    private SimpleShardDataTreeCohort cohort;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        doReturn(mockDataTree).when(mockShardDataTree).getDataTree();

        cohort = new SimpleShardDataTreeCohort(mockShardDataTree, mockModification, nextTransactionId(),
            mockUserCohorts);
    }

    @Test
    public void testCanCommitSuccess() throws Exception {
        ListenableFuture<Boolean> future = cohort.canCommit();
        assertNotNull("Future is null", future);
        assertEquals("Future", true, future.get(3, TimeUnit.SECONDS));
        verify(mockDataTree).validate(mockModification);
    }

    @Test(expected=OptimisticLockFailedException.class)
    public void testCanCommitWithConflictingModEx() throws Throwable {
        doThrow(new ConflictingModificationAppliedException(YangInstanceIdentifier.EMPTY, "mock")).
                when(mockDataTree).validate(mockModification);
        try {
            cohort.canCommit().get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected=TransactionCommitFailedException.class)
    public void testCanCommitWithDataValidationEx() throws Throwable {
        doThrow(new DataValidationFailedException(YangInstanceIdentifier.EMPTY, "mock")).
                when(mockDataTree).validate(mockModification);
        try {
            cohort.canCommit().get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void testCanCommitWithIllegalArgumentEx() throws Throwable {
        doThrow(new IllegalArgumentException("mock")).when(mockDataTree).validate(mockModification);
        try {
            cohort.canCommit().get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test
    public void testPreCommitAndCommitSuccess() throws Exception {
        DataTreeCandidateTip mockCandidate = mock(DataTreeCandidateTip.class);
        doReturn(mockCandidate ).when(mockDataTree).prepare(mockModification);

        cohort.preCommit(mockPreCallback);
        verify(mockDataTree).prepare(mockModification);

        assertSame("getCandidate", mockCandidate, cohort.getCandidate());

        cohort.commit(mockCommitCallback);
        verify(mockDataTree).commit(mockCandidate);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testPreCommitWithIllegalArgumentEx() throws Throwable {
        doThrow(new IllegalArgumentException("mock")).when(mockDataTree).prepare(mockModification);
        try {
            cohort.preCommit().get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void testCommitWithIllegalArgumentEx() throws Throwable {
        doThrow(new IllegalArgumentException("mock")).when(mockDataTree).commit(any(DataTreeCandidateTip.class));
        try {
            cohort.commit().get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test
    public void testAbort() throws Exception {
        cohort.abort().get();
    }
}
