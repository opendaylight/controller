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
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

class TestUtils {

    static final MemberName MEMBER_NAME = MemberName.forName("member-1");
    static final FrontendType FRONTEND_TYPE = FrontendType.forName("type-1");
    static final FrontendIdentifier FRONTEND_ID = FrontendIdentifier.create(MEMBER_NAME, FRONTEND_TYPE);
    static final ClientIdentifier CLIENT_ID = ClientIdentifier.create(FRONTEND_ID, 0);
    static final LocalHistoryIdentifier HISTORY_ID = new LocalHistoryIdentifier(CLIENT_ID, 0L);
    static final TransactionIdentifier TRANSACTION_ID = new TransactionIdentifier(HISTORY_ID, 0L);

    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws Exception;
    }

    private static final long TIMEOUT = 3;

    /**
     * Asserts, that future result when it completes is equal to given object.
     * Future must complete in {@link TestUtils#TIMEOUT} seconds.
     *
     * @param expected expected result
     * @param actual   future
     * @param <T>      type
     * @throws Exception exception
     */
    static <T> void assertFutureEquals(final T expected, final Future<T> actual) throws Exception {
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
    static <T> T getWithTimeout(final Future<T> future) throws Exception {
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
    static <T extends Throwable> T assertOperationThrowsException(final RunnableWithException operation,
                                                                  final Class<T> expectedException,
                                                                  final String message) throws Exception {
        try {
            operation.run();
            throw new AssertionError(message + expectedException);
        } catch (final Throwable e) {
            if (!e.getClass().equals(expectedException)) {
                throw e;
            }
            return (T) e;
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
    static <T extends Throwable> T assertOperationThrowsException(final RunnableWithException operation,
                                                                  final Class<T> expectedException)
            throws Exception {
        return assertOperationThrowsException(operation, expectedException, "Operation should throw exception: ");
    }
}
