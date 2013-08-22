
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.core.internal;

import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitch;
import org.openflow.protocol.OFMessage;

public class SwitchEvent {

    public static enum SwitchEventType {
        SWITCH_ADD, SWITCH_DELETE, SWITCH_ERROR, SWITCH_MESSAGE,
    }

    private SwitchEventType eventType;
    private ISwitch sw;
    private OFMessage msg;
    private int priority;

    public SwitchEvent(SwitchEventType type, ISwitch sw, OFMessage msg, int priority) {
        this.eventType = type;
        this.sw = sw;
        this.msg = msg;
        this.priority = priority;
    }

    public SwitchEventType getEventType() {
        return this.eventType;
    }

    public ISwitch getSwitch() {
        return this.sw;
    }

    public OFMessage getMsg() {
        return this.msg;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        String s;
        switch (this.eventType) {
        case SWITCH_ADD:
            s = "SWITCH_ADD";
            break;
        case SWITCH_DELETE:
            s = "SWITCH_DELETE";
            break;
        case SWITCH_ERROR:
            s = "SWITCH_ERROR";
            break;
        case SWITCH_MESSAGE:
            s = "SWITCH_MESSAGE";
            break;
        default:
            s = "?" + this.eventType.ordinal() + "?";
        }
        return "Switch Event: " + s;
    }
}
