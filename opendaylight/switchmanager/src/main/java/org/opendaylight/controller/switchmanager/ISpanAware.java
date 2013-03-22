
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager;

import java.util.List;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;

/**
 * The interface provides the methods for notifying when span ports
 * are configured/unconfigured.
 */
public interface ISpanAware {
    /**
     * This method is called when list of ports in a node are added/deleted as span ports.
     *
     * @param node		{@link org.opendaylight.controller.sal.core.Node} being updated
     * @param portList	list of span {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @param add		true if add; false if delete.
     */
    public void spanUpdate(Node node, List<NodeConnector> portList, boolean add);
}