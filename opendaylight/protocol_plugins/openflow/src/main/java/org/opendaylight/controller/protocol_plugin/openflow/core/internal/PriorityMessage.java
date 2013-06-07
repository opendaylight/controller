/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.core.internal;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.openflow.protocol.OFMessage;

/**
 * This class describes an OpenFlow message with priority
 */
class PriorityMessage {
    OFMessage msg;
    int priority;
    final static AtomicLong seq = new AtomicLong();
    final long seqNum;
    boolean syncReply; // set to true if we want to be blocked until the response arrives

    public PriorityMessage(OFMessage msg, int priority) {
        this.msg = msg;
        this.priority = priority;
        this.seqNum = seq.getAndIncrement();
        this.syncReply = false;
    }

    public PriorityMessage(OFMessage msg, int priority, boolean syncReply) {
        this(msg, priority);
        this.syncReply = syncReply;
    }

    public OFMessage getMsg() {
        return msg;
    }

    public void setMsg(OFMessage msg) {
        this.msg = msg;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((msg == null) ? 0 : msg.hashCode());
        result = prime * result + priority;
        result = prime * result + (int) (seqNum ^ (seqNum >>> 32));
        result = prime * result + (syncReply ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PriorityMessage other = (PriorityMessage) obj;
        if (msg == null) {
            if (other.msg != null)
                return false;
        } else if (!msg.equals(other.msg))
            return false;
        if (priority != other.priority)
            return false;
        if (seqNum != other.seqNum)
            return false;
        if (syncReply != other.syncReply)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PriorityMessage [msg=" + msg + ", priority=" + priority
                + ", seqNum=" + seqNum + ", syncReply=" + syncReply + "]";
    }
}
