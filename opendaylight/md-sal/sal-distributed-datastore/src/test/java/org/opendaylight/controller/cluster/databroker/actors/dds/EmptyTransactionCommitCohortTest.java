/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.TRANSACTION_ID;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.getWithTimeout;

import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class EmptyTransactionCommitCohortTest {
    @Mock
    private AbstractClientHistory history;

    private EmptyTransactionCommitCohort cohort;

    @Before
    public void setUp() {
        cohort = new EmptyTransactionCommitCohort(history, TRANSACTION_ID);
    }

    @Test
    public void testCanCommit() throws Exception {
        final ListenableFuture<Boolean> canCommit = cohort.canCommit();
        assertEquals(Boolean.TRUE, getWithTimeout(canCommit));
    }

    @Test
    public void testPreCommit() throws Exception {
        assertNotNull(getWithTimeout(cohort.preCommit()));
    }

    @Test
    public void testAbort() throws Exception {
        final ListenableFuture<?> abort = cohort.abort();
        verify(history).onTransactionComplete(TRANSACTION_ID);
        assertNotNull(getWithTimeout(abort));
    }

    @Test
    public void testCommit() throws Exception {
        final ListenableFuture<?> commit = cohort.commit();
        verify(history).onTransactionComplete(TRANSACTION_ID);
        assertNotNull(getWithTimeout(commit));
    }

}
