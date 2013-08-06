
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.core.internal;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitch;
import org.openflow.protocol.OFError;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;

public class StatisticsCollector implements Callable<Object> {

    private ISwitch sw;
    private Integer xid;
    private OFStatisticsRequest request;
    private CountDownLatch latch;
    private Object result;
    private List<OFStatistics> stats;

    public StatisticsCollector(ISwitch sw, int xid, OFStatisticsRequest request) {
        this.sw = sw;
        this.xid = xid;
        this.request = request;
        latch = new CountDownLatch(1);
        result = new Object();
        stats = new CopyOnWriteArrayList<OFStatistics>();
    }

    /*
     * accumulate the stats records in result
     * Returns: true: if this is the last record
     *                 false: more to come
     */
    public boolean collect(OFStatisticsReply reply) {
        synchronized (result) {
            stats.addAll(reply.getStatistics());
            if ((reply.getFlags() & 0x01) == 0) {
                // all stats are collected, done
                result = stats;
                return true;
            } else {
                // still waiting for more to come
                return false;
            }
        }
    }

    @Override
    public Object call() throws Exception {
        sw.asyncSend(request, xid);
        // free up the request as it is no longer needed
        request = null;
        // wait until all stats replies are received or timeout
        latch.await();
        return result;
    }

    public Integer getXid() {
        return this.xid;
    }

    public void wakeup() {
        this.latch.countDown();
    }

    public void wakeup(OFError errorMsg) {

    }
}
