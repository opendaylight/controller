/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Enqueue extends Action {
    private static final long serialVersionUID = 1L;
    public static final String NAME = "ENQUEUE";
    public static final Pattern PATTERN = Pattern.compile(NAME + "=(.*)", Pattern.CASE_INSENSITIVE);
    @XmlElement
    private NodeConnector port;
    @XmlElement
    private int queue;

    public Enqueue() {
        super(NAME);
    }

    public Enqueue(NodeConnector port) {
        super(NAME);
        this.port = port;
        this.queue = 0;
    }

    public Enqueue(NodeConnector port, int queue) {
        super(NAME);
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
        if (queue != 0) {
            return String.format("%s=(%s#%s)", NAME, port, queue);
        } else {
            return String.format("%s=%s", NAME, port);
        }
    }

    @Override
    public Enqueue fromString(String actionString, Node node) {
        Matcher matcher = PATTERN.matcher(removeSpaces(actionString));
        if (matcher.matches()) {
            boolean queueSpecified = matcher.group(1).matches("\\((.*)\\)");
            String group = queueSpecified ? matcher.group(1).replace("(", "").replace(")", "") : matcher.group(1);
            String values[] = group.split("#");
            // Accept node connector form
            NodeConnector nc = NodeConnector.fromString(values[0]);
            if (nc == null) {
                nc = NodeConnector.fromString(String.format("%s|%s@%s", node.getType(), values[0], node.toString()));
            }
            if (nc != null) {
                return (values.length > 1) ? new Enqueue(nc, Short.parseShort(values[1])) : new Enqueue(nc);
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((port == null) ? 0 : port.hashCode());
        result = prime * result + queue;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Enqueue)) {
            return false;
        }
        Enqueue other = (Enqueue) obj;
        if (port == null) {
            if (other.port != null) {
                return false;
            }
        } else if (!port.equals(other.port)) {
            return false;
        }
        if (queue != other.queue) {
            return false;
        }
        return true;
    }
}
