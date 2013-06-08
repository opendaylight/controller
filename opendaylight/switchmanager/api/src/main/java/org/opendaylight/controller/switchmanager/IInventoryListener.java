
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager;

import java.util.Map;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;

/**
 * Primary purpose of this interface is to provide methods to listen to inventory changes
 */
public interface IInventoryListener {
    /**
     * This method is called when some properties of a node are added/deleted/changed.
     *
     * @param node	 		{@link org.opendaylight.controller.sal.core.Node} being updated
     * @param type   		{@link org.opendaylight.controller.sal.core.UpdateType}
     * @param propMap   	map of {@link org.opendaylight.controller.sal.core.Property} such as
     * 						{@link org.opendaylight.controller.sal.core.Description} and/or
     * 						{@link org.opendaylight.controller.sal.core.Tier} etc.
     */
    public void notifyNode(Node node, UpdateType type,
            Map<String, Property> propMap);

    /**
     * This method is called when some properties of a node connector are added/deleted/changed.
     *
     * @param nodeConnector	{@link org.opendaylight.controller.sal.core.NodeConnector} being updated
     * @param type   		{@link org.opendaylight.controller.sal.core.UpdateType}
     * @param propMap   	map of {@link org.opendaylight.controller.sal.core.Property} such as
     * 						{@link org.opendaylight.controller.sal.core.Description} and/or
     * 						{@link org.opendaylight.controller.sal.core.State} etc.
     */
    public void notifyNodeConnector(NodeConnector nodeConnector,
            UpdateType type, Map<String, Property> propMap);
}
