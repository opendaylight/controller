
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

import org.opendaylight.controller.sal.core.Node;

/**
 * This interface defines the methods to be called when looking up custom node types
 *
 */
@Deprecated
public interface INodeFactory {
    /**
     * Method to get custom node types from protocol plugins
     *
     * @param nodeType
     *            {@Link org.opendaylight.controller.sal.core.Node} type
     *            string
     * @param nodeId
     *            {@Link org.opendaylight.controller.sal.core.Node} ID
     *            string
     * @return the custom {@Link
     *         org.opendaylight.controller.sal.core.Node}
     */
    public Node fromString(String nodeType, String nodeId);
}
