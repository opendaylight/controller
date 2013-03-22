
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;

/**
 * Utility class for creating a Node object
 *
 *
 *
 */
public abstract class NodeCreator {
    public static Node createOFNode(Long switchId) {
        try {
            return new Node(NodeIDType.OPENFLOW, switchId);
        } catch (ConstructionException e1) {
            e1.printStackTrace();
            return null;
        }
    }
}
