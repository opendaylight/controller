
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.arphandler;

public class ARPCacheEvent {
    private ARPEvent event;
    private boolean newReply;

    public ARPCacheEvent(ARPEvent event, boolean newReply) {
        super();
        this.event = event;
        this.newReply = newReply;
    }

    public ARPEvent getEvent() {
        return event;
    }

    public boolean isNewReply() {
        return newReply;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((event == null) ? 0 : event.hashCode());
        result = prime * result + (newReply ? 1231 : 1237);
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
        ARPCacheEvent other = (ARPCacheEvent) obj;
        if (event == null) {
            if (other.event != null)
                return false;
        } else if (!event.equals(other.event))
            return false;
        if (newReply != other.newReply)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ARPCacheEvent [event=" + event + ", newReply=" + newReply + "]";
    }
}
