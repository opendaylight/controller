
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import org.opendaylight.controller.sal.core.NodeConnector;

/**
 * Represents the action of sending the packet out of a physical port
 *
 *
 *
 */
public class Output extends AbstractParameterAction<NodeConnector> {

    public Output(NodeConnector port) {
        super(port);
    }
    @Deprecated
    public NodeConnector getPort() {
        return getValue();
    }

    @Override
    public String toString() {
        return "Output" + "[" + getValue().toString() + "]";
    }
}
