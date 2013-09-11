/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.devices.web;

public class PortJsonBean {
    String portId;
    String portName;
    String internalPortName;

    public PortJsonBean() {
        this.portId = null;
        this.portName = null;
        this.internalPortName = null;
    }

    public PortJsonBean(String id, String name, String internalName) {
        this.portId = id;
        this.portName = (name == null) ? internalName : name;
        this.internalPortName = internalName;
    }
}
