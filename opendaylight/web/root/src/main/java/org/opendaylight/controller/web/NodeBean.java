/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.web;

import org.opendaylight.controller.sal.core.Node;

/**
 * Information about a node connected to a controller to send to the UI frontend
 * @author andrekim
 */
public class NodeBean {
    private final String node;
    private final String description;

    public NodeBean(Node node) {
        this(node, node.toString());
    }

    public NodeBean(Node node, String description) {
        this.node = node.toString();
        this.description = description;
    }
}
