/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connection;

import org.opendaylight.controller.sal.core.Node;

/**
 * The interface describes methods to be implemented by SAL connection service
 */
public interface IPluginOutConnectionService {
    /**
     * Method to test if a node is local to a controller.
     *
     * @param node
     *            the given node {@link org.opendaylight.controller.sal.core.Node}
     * @return true if node is local to this controller. false otherwise.
     */
    public boolean isLocal(Node node);

    public ConnectionLocality getLocalityStatus(Node node);
}