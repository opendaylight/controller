
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.discovery;

import java.util.Set;

import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;

/**
 * The interface class provides the methods to notify the listener when an edge
 * is added/deleted/changed
 */
public interface IDiscoveryService {
    /**
     * The methods is called when an edge is added/deleted/changed
     *
     * @param edge	 		{@link org.opendaylight.controller.sal.core.Edge} being updated
     * @param type   		{@link org.opendaylight.controller.sal.core.UpdateType}
     * @param props   		set of {@link org.opendaylight.controller.sal.core.Property} like
     * 						{@link org.opendaylight.controller.sal.core.Bandwidth} and/or
     * 						{@link org.opendaylight.controller.sal.core.Latency} etc.
     */
    public void notifyEdge(Edge edge, UpdateType type, Set<Property> props);
}
