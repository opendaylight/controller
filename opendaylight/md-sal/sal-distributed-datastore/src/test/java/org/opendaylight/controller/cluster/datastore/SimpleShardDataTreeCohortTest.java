/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ConflictingModificationAppliedException;
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

    private SimpleShardDataTreeCohort cohort;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        doReturn(mockDataTree).when(mockShardDataTree).getDataTree();

        cohort = new SimpleShardDataTreeCohort(mockShardDataTree, mockModification, nextTransactionId());
    }

    @Test
    public void testCanCommitSuccess() throws Exception {
        assertEquals("Future", true, cohort.canCommit());
        verify(mockDataTree).validate(mockModification);
    }

    @Test(expected=OptimisticLockFailedException.class)
    public void testCanCommitWithConflictingModEx() throws Exception {
        doThrow(new ConflictingModificationAppliedException(YangInstanceIdentifier.EMPTY, "mock")).
                when(mockDataTree).validate(mockModification);
        cohort.canCommit();
    }

    @Test(expected=TransactionCommitFailedException.class)
    public void testCanCommitWithDataValidationEx() throws Exception {
        doThrow(new DataValidationFailedException(YangInstanceIdentifier.EMPTY, "mock")).
                when(mockDataTree).validate(mockModification);
        cohort.canCommit();
    }

    @Test(expected=IllegalArgumentException.class)
    public void testCanCommitWithIllegalArgumentEx() throws Exception {
        doThrow(new IllegalArgumentException("mock")).when(mockDataTree).validate(mockModification);
        cohort.canCommit();
    }

    @Test
    public void testPreCommitAndCommitSuccess() throws Exception {
        DataTreeCandidateTip mockCandidate = mock(DataTreeCandidateTip.class);
        doReturn(mockCandidate ).when(mockDataTree).prepare(mockModification);

        cohort.preCommit();
        verify(mockDataTree).prepare(mockModification);

        assertSame("getCandidate", mockCandidate, cohort.getCandidate());

        cohort.commit();
        verify(mockDataTree).commit(mockCandidate);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testPreCommitWithIllegalArgumentEx() throws Exception {
        doThrow(new IllegalArgumentException("mock")).when(mockDataTree).prepare(mockModification);
        cohort.preCommit();
    }

    @Test(expected=IllegalArgumentException.class)
    public void testCommitWithIllegalArgumentEx() throws Exception {
        doThrow(new IllegalArgumentException("mock")).when(mockDataTree).commit(any(DataTreeCandidateTip.class));
        cohort.commit();
    }

    @Test
    public void testAbort() throws Exception {
        cohort.abort();
    }
}
