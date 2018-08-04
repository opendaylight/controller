/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.opendaylight.yangtools.util.concurrent.ExceptionMapper;

/**
 * Unit tests for MappingCheckedFuture.
 *
 * @author Thomas Pantelis
 */
public class MappingCheckedFutureTest {

    interface FutureInvoker {
        void invokeGet(CheckedFuture<?,?> future) throws Exception;

        Throwable extractWrappedTestEx(Exception from);
    }

    static class TestException extends Exception {
        private static final long serialVersionUID = 1L;

        TestException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    static final ExceptionMapper<TestException> MAPPER = new ExceptionMapper<TestException>(
                                                                      "Test", TestException.class) {

        @Override
        protected TestException newWithCause(final String message, final Throwable cause) {
            return new TestException(message, cause);
        }
    };

    static final FutureInvoker GET = new FutureInvoker() {
        @Override
        public void invokeGet(final CheckedFuture<?, ?> future) throws Exception {
            future.get();
        }

        @Override
        public Throwable extractWrappedTestEx(final Exception from) {
            if (from instanceof ExecutionException) {
                return from.getCause();
            }

            return from;
        }
    };

    static final FutureInvoker TIMED_GET = new FutureInvoker() {
        @Override
        public void invokeGet(final CheckedFuture<?, ?> future) throws Exception {
            future.get(1, TimeUnit.HOURS);
        }

        @Override
        public Throwable extractWrappedTestEx(final Exception from) {
            if (from instanceof ExecutionException) {
                return from.getCause();
            }

            return from;
        }
    };

    static final FutureInvoker CHECKED_GET = new FutureInvoker() {
        @Override
        public void invokeGet(final CheckedFuture<?,?> future) throws Exception {
            future.checkedGet();
        }

        @Override
        public Throwable extractWrappedTestEx(final Exception from) {
            return from;
        }
    };

    static final FutureInvoker TIMED_CHECKED_GET = new FutureInvoker() {
        @Override
        public void invokeGet(final CheckedFuture<?,?> future) throws Exception {
            future.checkedGet(50, TimeUnit.MILLISECONDS);
        }

        @Override
        public Throwable extractWrappedTestEx(final Exception from) {
            return from;
        }
    };

    @Test
    public void testGet() throws Exception {
        SettableFuture<String> delegate = SettableFuture.create();
        MappingCheckedFuture<String,TestException> future = MappingCheckedFuture.create(delegate, MAPPER);
        delegate.set("test");
        assertEquals("get", "test", future.get());
    }

    @Test
    public void testGetWithExceptions() throws Exception {
        testExecutionException(GET, new RuntimeException());
        testExecutionException(GET, new TestException("mock", null));
        testCancellationException(GET);
        testInterruptedException(GET);
    }

    @Test
    public void testTimedGet() throws Exception {
        SettableFuture<String> delegate = SettableFuture.create();
        MappingCheckedFuture<String,TestException> future = MappingCheckedFuture.create(delegate, MAPPER);
        delegate.set("test");
        assertEquals("get", "test", future.get(50, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testTimedGetWithExceptions() throws Exception {
        testExecutionException(TIMED_GET, new RuntimeException());
        testCancellationException(TIMED_GET);
        testInterruptedException(TIMED_GET);
    }

    @Test
    public void testCheckedGetWithExceptions() throws Exception {
        testExecutionException(CHECKED_GET, new RuntimeException());
        testCancellationException(CHECKED_GET);
        testInterruptedException(CHECKED_GET);
    }

    @Test
    public void testTimedCheckedWithExceptions() throws Exception {
        testExecutionException(TIMED_CHECKED_GET, new RuntimeException());
        testCancellationException(TIMED_CHECKED_GET);
        testInterruptedException(TIMED_CHECKED_GET);
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    private static void testExecutionException(final FutureInvoker invoker, final Throwable cause) {
        SettableFuture<String> delegate = SettableFuture.create();
        MappingCheckedFuture<String, TestException> mappingFuture = MappingCheckedFuture.create(delegate, MAPPER);

        delegate.setException(cause);

        try {
            invoker.invokeGet(mappingFuture);
            fail("Expected exception thrown");
        } catch (Exception e) {
            Throwable expectedTestEx = invoker.extractWrappedTestEx(e);
            assertNotNull("Expected returned exception is null", expectedTestEx);
            assertEquals("Exception type", TestException.class, expectedTestEx.getClass());

            if (cause instanceof TestException) {
                assertNull("Expected null cause", expectedTestEx.getCause());
            } else {
                assertSame("TestException cause", cause, expectedTestEx.getCause());
            }
        }
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    private static void testCancellationException(final FutureInvoker invoker) {
        SettableFuture<String> delegate = SettableFuture.create();
        MappingCheckedFuture<String, TestException> mappingFuture = MappingCheckedFuture.create(delegate, MAPPER);

        mappingFuture.cancel(false);

        try {
            invoker.invokeGet(mappingFuture);
            fail("Expected exception thrown");
        } catch (Exception e) {
            Throwable expectedTestEx = invoker.extractWrappedTestEx(e);
            assertNotNull("Expected returned exception is null", expectedTestEx);
            assertEquals("Exception type", TestException.class, expectedTestEx.getClass());
            assertEquals("TestException cause type", CancellationException.class, expectedTestEx.getCause().getClass());
        }
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    private static void testInterruptedException(final FutureInvoker invoker) throws Exception {
        SettableFuture<String> delegate = SettableFuture.create();
        final MappingCheckedFuture<String, TestException> mappingFuture = MappingCheckedFuture.create(delegate, MAPPER);

        final AtomicReference<AssertionError> assertError = new AtomicReference<>();
        final CountDownLatch doneLatch = new CountDownLatch(1);
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    doInvoke();
                } catch (AssertionError e) {
                    assertError.set(e);
                } finally {
                    doneLatch.countDown();
                }
            }

            void doInvoke() {
                try {
                    invoker.invokeGet(mappingFuture);
                    fail("Expected exception thrown");
                } catch (Exception e) {
                    Throwable expectedTestEx = invoker.extractWrappedTestEx(e);
                    assertNotNull("Expected returned exception is null", expectedTestEx);
                    assertEquals("Exception type", TestException.class, expectedTestEx.getClass());
                    assertEquals("TestException cause type", InterruptedException.class,
                                  expectedTestEx.getCause().getClass());
                }
            }
        };
        thread.start();

        thread.interrupt();
        assertTrue("get call completed", doneLatch.await(5, TimeUnit.SECONDS));

        if (assertError.get() != null) {
            throw assertError.get();
        }
    }
}
