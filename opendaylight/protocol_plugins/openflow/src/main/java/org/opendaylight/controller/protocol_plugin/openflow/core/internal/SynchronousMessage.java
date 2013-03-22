
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.core.internal;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitch;
import org.openflow.protocol.OFBarrierRequest;
import org.openflow.protocol.OFError;
import org.openflow.protocol.OFMessage;

/**
 * Implements the synchronous message send to a switch
 * It sends the requested message to the switch followed by a barrier request message
 * It returns the result once it gets the reply from the switch or after a timeout
 * If the protocol does not dictate the switch to reply the processing status for a particular message
 * the barrier request forces the switch to reply saying whether or not the message processing was
 * successful for messages sent to the switch up to this point
 *
 *
 *
 */
public class SynchronousMessage implements Callable<Object> {
    private ISwitch sw;
    private Integer xid;
    private OFMessage syncMsg;
    protected CountDownLatch latch;
    private Object result;

    public SynchronousMessage(ISwitch sw, Integer xid, OFMessage msg) {
        this.sw = sw;
        this.xid = xid;
        syncMsg = msg;
        latch = new CountDownLatch(1);
        result = null;
    }

    @Override
    public Object call() throws Exception {
        sw.asyncSend(syncMsg, xid);
        OFBarrierRequest barrierMsg = new OFBarrierRequest();
        sw.asyncSend(barrierMsg, xid);
        latch.await();
        return result;
    }

    public Integer getXid() {
        return this.xid;
    }

    public void wakeup() {
        this.latch.countDown();
    }

    public void wakeup(OFError e) {
        result = e;
        wakeup();
    }

}