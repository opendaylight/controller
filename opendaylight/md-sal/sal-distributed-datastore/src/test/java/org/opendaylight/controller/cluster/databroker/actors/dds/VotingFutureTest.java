/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

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

    private static final int TIMEOUT = 3;

    private Object result;
    private ScheduledExecutorService executor;
    private VotingFuture<Object> future;

    @Before
    public void setUp() throws Exception {
        result = new Object();
        future = new VotingFuture<>(result, 3);
        executor = Executors.newScheduledThreadPool(1);
    }

    @After
    public void tearDown() throws Exception {
        executor.shutdownNow();
    }

    @Test
    public void testTrivialCases() throws Exception {
        final VotingFuture<Object> oneYesVoteFuture = new VotingFuture<>(result, 1);
        oneYesVoteFuture.voteYes();
        checkSuccess(oneYesVoteFuture, result);
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
        checkSuccess(future, result);
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
        checkSuccess(future, result);
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
        try {
            future.get(TIMEOUT, TimeUnit.SECONDS);
            Assert.fail("ExecutionException expected");
        } catch (final ExecutionException e) {
            //first no is set as cause
            Assert.assertEquals(cause1, e.getCause());
            //subsequent no causes are added as suppressed
            final Throwable[] suppressed = e.getCause().getSuppressed();
            Assert.assertEquals(1, suppressed.length);
            Assert.assertEquals(cause2, suppressed[0]);
        }
    }

    private static void checkException(final Future future, final RuntimeException cause) throws Exception {
        try {
            future.get(TIMEOUT, TimeUnit.SECONDS);
            Assert.fail("ExecutionException expected");
        } catch (final ExecutionException e) {
            Assert.assertEquals(cause, e.getCause());
        }
    }

    private static void checkSuccess(final Future future, final Object result) throws Exception {
        Assert.assertEquals(result, future.get(TIMEOUT, TimeUnit.SECONDS));
    }

}