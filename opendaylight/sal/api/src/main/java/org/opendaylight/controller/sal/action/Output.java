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

/**
 * Represents the action of sending the packet out of a physical port
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Output extends Action {
    private static final long serialVersionUID = 1L;
    public static final String NAME = "OUTPUT";
    public static final Pattern PATTERN = Pattern.compile(NAME + "=(.*)", Pattern.CASE_INSENSITIVE);
    @XmlElement
    private NodeConnector port;

    /**
     * Internally used constructor. Will generate an invalid Output action
     */
    public Output() {
        super(NAME);
    }

    public Output(NodeConnector port) {
        super(NAME);
        this.port = port;
    }

    public NodeConnector getPort() {
        return port;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((port == null) ? 0 : port.hashCode());
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
        if (!(obj instanceof Output)) {
            return false;
        }
        Output other = (Output) obj;
        if (port == null) {
            if (other.port != null) {
                return false;
            }
        } else if (!port.equals(other.port)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return NAME + "=" + port;
    }

    @Override
    public Output fromString(String actionString, Node node) {
        Matcher matcher = PATTERN.matcher(removeSpaces(actionString));
        if (matcher.matches()) {
            String value = matcher.group(1);
            // Accept node connector form
            NodeConnector nc = NodeConnector.fromString(value);
            if (nc == null) {
                // Fall back to port number form
                nc = NodeConnector.fromString(String.format("%s|%s@%s", node.getType(), value, node.toString()));
            }
            if (nc != null) {
                return new Output(nc);
            }
        }
        return null;
    }

    @Override
    public boolean isValid() {
        return port != null;
    }
}
