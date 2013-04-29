
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager;

import org.opendaylight.controller.sal.core.Node;

import org.opendaylight.controller.switchmanager.Subnet;

/**
 * The interface class provides methods to notify listeners about subnet and
 * mode changes.
 */
public interface ISwitchManagerAware {
    /**
     * The method is called when subnet is added/deleted
     *
     * @param sub {@link org.opendaylight.controller.switchmanager.Subnet}
     * @param add true if add; false if delete.
     */
    public void subnetNotify(Subnet sub, boolean add);

    /**
     * The method is called when proactive/reactive mode is changed in a node
     *
     * @param node {@link org.opendaylight.controller.sal.core.Node}
     * @param proactive true if mode is set as proactive; false if mode is reactive.
     */
    public void modeChangeNotify(Node node, boolean proactive);
}
