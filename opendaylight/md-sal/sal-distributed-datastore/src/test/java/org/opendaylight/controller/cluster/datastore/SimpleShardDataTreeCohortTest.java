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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.opendaylight.controller.cluster.datastore.ShardDataTreeMocking.immediateCanCommit;
import static org.opendaylight.controller.cluster.datastore.ShardDataTreeMocking.immediatePreCommit;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
        doReturn(Optional.empty()).when(mockUserCohorts).abort();

        cohort = new SimpleShardDataTreeCohort(mockShardDataTree, mockModification, nextTransactionId(),
            mockUserCohorts);
    }

    @Test
    public void testCanCommitSuccess() throws Exception {
        immediateCanCommit(cohort);
        verify(mockDataTree).validate(mockModification);
    }

    private void testValidatationPropagates(final Exception cause) throws DataValidationFailedException {
        doThrow(cause).when(mockDataTree).validate(mockModification);

        @SuppressWarnings("unchecked")
        final FutureCallback<Void> callback = mock(FutureCallback.class);
        cohort.canCommit(callback);

        verify(callback).onFailure(cause);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void testCanCommitWithConflictingModEx() throws DataValidationFailedException {
        testValidatationPropagates(new ConflictingModificationAppliedException(YangInstanceIdentifier.EMPTY, "mock"));
    }

    @Test
    public void testCanCommitWithDataValidationEx() throws DataValidationFailedException {
        testValidatationPropagates(new DataValidationFailedException(YangInstanceIdentifier.EMPTY, "mock"));
    }

    @Test
    public void testCanCommitWithIllegalArgumentEx() throws DataValidationFailedException {
        testValidatationPropagates(new IllegalArgumentException("mock"));
    }

    @Test
    public void testPreCommitAndCommitSuccess() {
        DataTreeCandidateTip mockCandidate = mock(DataTreeCandidateTip.class);
        doReturn(mockCandidate).when(mockDataTree).prepare(mockModification);

        immediateCanCommit(cohort);
        cohort.preCommit(mockPreCallback);
        verify(mockDataTree).prepare(mockModification);

        assertSame("getCandidate", mockCandidate, cohort.getCandidate());

        cohort.commit(mockCommitCallback);
        verify(mockDataTree).commit(mockCandidate);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testPreCommitWithIllegalArgumentEx() throws Throwable {
        final Exception ex = new IllegalArgumentException("mock");
        doThrow(ex).when(mockDataTree).prepare(mockModification);

        immediateCanCommit(cohort);

        @SuppressWarnings("unchecked")
        final FutureCallback<DataTreeCandidate> callback = mock(FutureCallback.class);
        cohort.preCommit(callback);

        verify(callback).onFailure(ex);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void testCommitWithIllegalArgumentEx() {
        final Exception ex = new IllegalArgumentException("mock");
        doThrow(ex).when(mockDataTree).commit(any(DataTreeCandidateTip.class));

        immediateCanCommit(cohort);
        immediatePreCommit(cohort);

        @SuppressWarnings("unchecked")
        final FutureCallback<UnsignedLong> callback = mock(FutureCallback.class);
        cohort.commit(callback);

        verify(callback).onFailure(ex);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void testAbort() throws Exception {
        cohort.abort().get();
    }
}
