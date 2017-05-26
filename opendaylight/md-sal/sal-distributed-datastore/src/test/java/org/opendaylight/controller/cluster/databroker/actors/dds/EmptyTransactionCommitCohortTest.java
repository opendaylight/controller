/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.TRANSACTION_ID;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.getWithTimeout;

import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class EmptyTransactionCommitCohortTest {

    @Mock
    private AbstractClientHistory history;

    private EmptyTransactionCommitCohort cohort;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        cohort = new EmptyTransactionCommitCohort(history, TRANSACTION_ID);
    }

    @Test
    public void testCanCommit() throws Exception {
        final ListenableFuture<Boolean> canCommit = cohort.canCommit();
        Assert.assertTrue(getWithTimeout(canCommit));
    }

    @Test
    public void testPreCommit() throws Exception {
        final ListenableFuture<Void> preCommit = cohort.preCommit();
        Assert.assertNull(getWithTimeout(preCommit));
    }

    @Test
    public void testAbort() throws Exception {
        final ListenableFuture<Void> abort = cohort.abort();
        verify(history).onTransactionComplete(TRANSACTION_ID);
        Assert.assertNull(getWithTimeout(abort));
    }

    @Test
    public void testCommit() throws Exception {
        final ListenableFuture<Void> commit = cohort.commit();
        verify(history).onTransactionComplete(TRANSACTION_ID);
        Assert.assertNull(getWithTimeout(commit));
    }

}