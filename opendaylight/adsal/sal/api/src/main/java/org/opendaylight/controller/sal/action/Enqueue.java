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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.NodeConnector;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class Enqueue extends Action {
    private static final long serialVersionUID = 1L;
    @XmlElement
    private NodeConnector port;
    @XmlElement
    private int queue;

    /* Dummy constructor for JAXB */
    @SuppressWarnings("unused")
    private Enqueue() {
    }

    public Enqueue(NodeConnector port) {
        type = ActionType.ENQUEUE;
        this.port = port;
        this.queue = 0;
    }

    public Enqueue(NodeConnector port, int queue) {
        type = ActionType.ENQUEUE;
        this.port = port;
        this.queue = queue;
    }

    public NodeConnector getPort() {
        return port;
    }

    public int getQueue() {
        return queue;
    }

    @Override
    public String toString() {
        return String.format("%s[%s:%s]", type, port, queue);
    }
}
