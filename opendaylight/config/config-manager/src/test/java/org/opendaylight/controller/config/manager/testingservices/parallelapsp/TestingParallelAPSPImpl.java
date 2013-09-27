/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.parallelapsp;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Closeable;
import java.io.IOException;

import javax.annotation.concurrent.NotThreadSafe;

import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingThreadPoolIfc;

import com.google.common.base.Strings;

@NotThreadSafe
public class TestingParallelAPSPImpl implements TestingAPSP, Closeable {
    public static final int MINIMAL_NUMBER_OF_THREADS = 10;
    private TestingThreadPoolIfc threadPool;
    private String someParam;

    public TestingParallelAPSPImpl(TestingThreadPoolIfc threadPool,
            String someParam) {
        checkArgument(
                threadPool.getMaxNumberOfThreads() >= MINIMAL_NUMBER_OF_THREADS,
                "Parameter 'threadPool' has not enough threads");
        checkArgument(Strings.isNullOrEmpty(someParam) == false,
                "Parameter 'someParam' is blank");
        this.threadPool = threadPool;
        this.someParam = someParam;
    }

    @Override
    public int getMaxNumberOfThreads() {
        return threadPool.getMaxNumberOfThreads();
    }

    @Override
    public void close() throws IOException {

    }

    TestingThreadPoolIfc getThreadPool() {
        return threadPool;
    }

    void setSomeParam(String s) {
        checkArgument(Strings.isNullOrEmpty(someParam) == false,
                "Parameter 'someParam' is blank");
        this.someParam = s;
    }

    public String getSomeParam() {
        return someParam;
    }

}
