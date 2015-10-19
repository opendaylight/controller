/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.topology;

import com.google.common.annotations.Beta;

/**
 * A customizable strategy that gets executed when a BaseTopologyManager|BaseNodeManager is created.
 * If the election should be executed at another moment, you need to pass the NoopRoleChangeStrategy into the Manager
 * and the role candidate registration needs to happen in your implemented Node/Topology callback
 */
@Beta
public interface RoleChangeStrategy extends RoleChangeListener {

    /**
     * Your pre-election and election logic goes here, e.g you should register your candidate into the ElectionService
     *
     * @param electionCandidate NodeListener that should receive the subsequent onRoleChanged callback
     *            when a role change occurs.
     */
    void registerRoleCandidate(NodeListener electionCandidate);

    /**
     * Invoke whenever you want to stop candidate from partaking in election.
     */
    void unregisterRoleCandidate();

}

