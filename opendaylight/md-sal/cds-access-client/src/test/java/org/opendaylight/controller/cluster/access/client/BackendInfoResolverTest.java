/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.access.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BackendInfoResolverTest {

    @Mock
    BackendInfo mockBackedInfo;

    ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();

    private TestBackedInfoResolver backedInfoResolver;
    private Long mockCookies;

    @Before
    public void initialization() {
        MockitoAnnotations.initMocks(this);
        mockCookies = ThreadLocalRandom.current().nextLong();
        backedInfoResolver = new TestBackedInfoResolver();
    }

    @Test(expected = NullPointerException.class)
    public void testGetFutureBackendInfoNullCookie() {
        backedInfoResolver.getFutureBackendInfo(null);
    }

    @Test
    public void testGetFutureBackendInfoSync() throws Exception {
        mockCookies = 0L;
        final Optional<BackendInfo> optBackedInfo = backedInfoResolver.getFutureBackendInfo(mockCookies);
        final CompletionStage<? extends BackendInfo> backedInfo = backedInfoResolver.getBackendInfo(mockCookies);
        final BackendInfo futureBacadInfo = backedInfo.toCompletableFuture().get();
        assertSame(futureBacadInfo, mockBackedInfo);
        assertSame(backedInfo.toCompletableFuture().get(), mockBackedInfo);
        assertTrue(optBackedInfo.isPresent());
        assertSame(optBackedInfo.get(), mockBackedInfo);
    }

    @Test
    public void testGetFutureBackendInfoAsync() throws Exception {
        final Optional<BackendInfo> optBackedInfo = backedInfoResolver.getFutureBackendInfo(mockCookies);
        assertFalse(optBackedInfo.isPresent());
        final CompletionStage<? extends BackendInfo> backedInfo = backedInfoResolver.getBackendInfo(mockCookies);
        final BackendInfo futureBacadInfo = backedInfo.toCompletableFuture().get();
        assertSame(futureBacadInfo, mockBackedInfo);
        assertSame(backedInfo.toCompletableFuture().get(), mockBackedInfo);
        assertFalse(backedInfo.toCompletableFuture().isCompletedExceptionally());
    }

    @Test
    public void testInvalidateBackend() throws Exception {
        backedInfoResolver.getFutureBackendInfo(mockCookies);
        final CompletionStage<? extends BackendInfo> backedInfo = backedInfoResolver.getBackendInfo(mockCookies);
        backedInfoResolver.invalidateBackend(mockCookies, backedInfo);
        assertTrue(backedInfo.toCompletableFuture().isCompletedExceptionally());
    }

    private class TestBackedInfoResolver extends BackendInfoResolver<BackendInfo> {

        @Override
        protected CompletableFuture<BackendInfo> resolveBackendInfo(final Long cookie) {
            if (cookie == 0L) {
                return CompletableFuture.<BackendInfo> completedFuture(mockBackedInfo);
            }
            final CompletableFuture<BackendInfo> future = new CompletableFuture<>();
            ex.schedule(() -> {
                if (!future.isDone()) {
                    future.complete(mockBackedInfo);
                }
            }, 1, TimeUnit.SECONDS);
            return future;
        }

        @Override
        protected void invalidateBackendInfo(final CompletionStage<? extends BackendInfo> info) {
            info.toCompletableFuture().completeExceptionally(
                    new InterruptedException("Test Object interupt implementation functionality!"));
        }

    }
}
