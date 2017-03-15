/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;

public class TestUtils {

    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws Exception;
    }

    public static final long TIMEOUT = 3;

    /**
     * Asserts, that future result when it completes is equal to given object.
     * Future must complete in {@link TestUtils#TIMEOUT} seconds.
     *
     * @param expected expected result
     * @param actual   future
     * @param <T>      type
     * @throws Exception exception
     */
    public static <T> void assertFutureEquals(final T expected, final Future<T> actual) throws Exception {
        Assert.assertEquals(expected, getWithTimeout(actual));
    }

    /**
     * Calls {@link Future#get(long, TimeUnit)} with {@link TestUtils#TIMEOUT} in seconds.
     *
     * @param future future
     * @param <T>    type
     * @return future result
     * @throws Exception exception
     */
    public static <T> T getWithTimeout(final Future<T> future) throws Exception {
        return future.get(TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Asserts that given operation invocation, will throw an exception of given class.
     *
     * @param operation         operation
     * @param expectedException expected exception class
     * @param message           message, when expected exception isn't thrown
     * @return expected exception instance. Can be used for additional assertions.
     * @throws Exception unexpected exception.
     */
    //Throwable is propagated if doesn't match the expected type
    @SuppressWarnings("checkstyle:IllegalCatch")
    public static Throwable assertOperationThrowsException(final RunnableWithException operation,
                                                           final Class<? extends Throwable> expectedException,
                                                           final String message) throws Exception {
        try {
            operation.run();
            throw new AssertionError(message + expectedException);
        } catch (final Throwable e) {
            if (!e.getClass().equals(expectedException)) {
                throw e;
            }
            return e;
        }
    }

    /**
     * Asserts, that when given operation is run, exception of given class is thrown.
     *
     * @param operation         operation
     * @param expectedException expected exception class
     * @return expected exception instance. Can be used for additional assertions.
     * @throws Exception unexpected exception.
     */
    public static Throwable assertOperationThrowsException(final RunnableWithException operation,
                                                           final Class<? extends Throwable> expectedException) throws Exception {
        return assertOperationThrowsException(operation, expectedException, "Operation should throw exception: ");
    }
}
