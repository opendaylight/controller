/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.assertFutureEquals;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.assertOperationThrowsException;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.getWithTimeout;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class VotingFutureTest {

    private Object result;
    private ScheduledExecutorService executor;
    private VotingFuture<Object> future;

    @Before
    public void setUp() {
        result = new Object();
        future = new VotingFuture<>(result, 3);
        executor = Executors.newScheduledThreadPool(1);
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void testTrivialCases() throws Exception {
        final VotingFuture<Object> oneYesVoteFuture = new VotingFuture<>(result, 1);
        oneYesVoteFuture.voteYes();
        assertFutureEquals(result, oneYesVoteFuture);
        final VotingFuture<Object> oneNoVoteFuture = new VotingFuture<>(result, 1);
        final RuntimeException cause = new RuntimeException("fail");
        oneNoVoteFuture.voteNo(cause);
        checkException(oneNoVoteFuture, cause);
    }

    @Test
    public void testVoteYes() throws Exception {
        future.voteYes();
        future.voteYes();
        future.voteYes();
        assertFutureEquals(result, future);
    }

    @Test
    public void testVoteYesBlocking() throws Exception {
        final AtomicBoolean voted = new AtomicBoolean(false);
        future.voteYes();
        future.voteYes();
        executor.schedule(() -> {
            voted.set(true);
            future.voteYes();
        }, 1, TimeUnit.SECONDS);
        assertFutureEquals(result, future);
        Assert.assertTrue("Future completed before vote", voted.get());
    }

    @Test
    public void testVoteNo() throws Exception {
        future.voteYes();
        final RuntimeException cause = new RuntimeException("fail");
        future.voteNo(cause);
        future.voteYes();
        checkException(future, cause);
    }

    @Test
    public void testVoteNoFirst() throws Exception {
        final RuntimeException cause = new RuntimeException("fail");
        future.voteNo(cause);
        future.voteYes();
        future.voteYes();
        checkException(future, cause);
    }

    @Test
    public void testVoteNoLast() throws Exception {
        future.voteYes();
        future.voteYes();
        final RuntimeException cause = new RuntimeException("fail");
        future.voteNo(cause);
        checkException(future, cause);
    }

    @Test
    public void testVoteNoBlocking() throws Exception {
        final AtomicBoolean voted = new AtomicBoolean(false);
        future.voteYes();
        final RuntimeException cause = new RuntimeException("fail");
        future.voteNo(cause);
        executor.schedule(() -> {
            voted.set(true);
            future.voteYes();
        }, 1, TimeUnit.SECONDS);
        checkException(future, cause);
        Assert.assertTrue("Future completed before vote", voted.get());
    }

    @Test
    public void testMultipleVoteNo() throws Exception {
        future.voteYes();
        final RuntimeException cause1 = new RuntimeException("fail");
        final RuntimeException cause2 = new RuntimeException("fail");
        future.voteNo(cause1);
        future.voteNo(cause2);
        final Throwable thrown = assertOperationThrowsException(() -> getWithTimeout(future), ExecutionException.class);
        //first no is set as cause
        Assert.assertEquals(cause1, thrown.getCause());
        //subsequent no causes are added as suppressed
        final Throwable[] suppressed = thrown.getCause().getSuppressed();
        Assert.assertEquals(1, suppressed.length);
        Assert.assertEquals(cause2, suppressed[0]);
    }

    private static void checkException(final Future<Object> future, final RuntimeException cause) throws Exception {
        final Throwable thrown = assertOperationThrowsException(() -> getWithTimeout(future), ExecutionException.class);
        Assert.assertEquals(cause, thrown.getCause());
    }

}
