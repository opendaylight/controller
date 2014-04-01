/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.Node;

/**
 * Represents the action of flooding the packet out all the physical ports
 * except the input port
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class FloodAll extends Action {
    private static final long serialVersionUID = 1L;
    public static final String NAME = "FLOOD_ALL";

    public FloodAll() {
        super(NAME);
    }

    @Override
    public FloodAll fromString(String actionString, Node node) {
        return (actionString != null && (actionString.trim().equalsIgnoreCase(NAME))) ? this : null;
    }
}
