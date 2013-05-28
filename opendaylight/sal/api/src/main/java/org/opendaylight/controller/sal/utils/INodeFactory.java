
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
 * @file   INodeFactory.java
 *
 * @brief  Define the interface to be called when looking up custom node types
 *
 */

public interface INodeFactory {
    /**
     * Method to get custom node types from protocol plugins
     *
     */
    public Node fromString(String nodeId, String nodeType);
}
