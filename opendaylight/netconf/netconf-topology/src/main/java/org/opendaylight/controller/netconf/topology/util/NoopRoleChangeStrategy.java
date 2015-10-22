/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.topology.util;

import org.opendaylight.controller.netconf.topology.NodeListener;
import org.opendaylight.controller.netconf.topology.RoleChangeStrategy;

/**
 * Use this strategy to override the default roleChange registration's in BaseTopologyManager|BaseNodeManager
 * If you use this, you will need to execute your own election in your implemented callbacks.
 */
public class NoopRoleChangeStrategy implements RoleChangeStrategy {

    @Override
    public void registerRoleCandidate(final NodeListener electionCandidate) {

    };

    @Override
    public void unregisterRoleCandidate() {

    };

    @Override
    public void onRoleChanged(final RoleChangeDTO roleChangeDTO) {

    };
}
