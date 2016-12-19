/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.benchmark.sharding.impl.tests;

/** Statistics generated from a shard test at the end of a test run.
 * @author jmedved
 *
 */
public class ShardTestStats {
    /** The status from the test run, OK or ERROR.
     * @author jmedved
     *
     */
    public enum TestStatus { OK, ERROR }

    private final TestStatus status;
    private final long txOk;
    private final long txError;
    private final long txSubmitted;
    private final long execTime;
    private final long listenerEventsOk;
    private final long listenerEventsFail;

    ShardTestStats(TestStatus status, long txOk, long txError, long txSubmitted, long execTime,
            long dataTreeEventsOk, long dataTreeEventsFail) {
        this.status = status;
        this.txOk = txOk;
        this.txError = txError;
        this.txSubmitted = txSubmitted;
        this.execTime = execTime;
        this.listenerEventsOk = dataTreeEventsOk;
        this.listenerEventsFail = dataTreeEventsFail;
    }

    public TestStatus getStatus() {
        return status;
    }

    public long getTxOk() {
        return txOk;
    }

    public long getTxError() {
        return txError;
    }

    public long getTxSubmitted() {
        return txSubmitted;
    }

    public long getExecTime() {
        return execTime;
    }

    public long getListenerEventsOk() {
        return listenerEventsOk;
    }

    public long getListenerEventsFail() {
        return listenerEventsFail;
    }

}
