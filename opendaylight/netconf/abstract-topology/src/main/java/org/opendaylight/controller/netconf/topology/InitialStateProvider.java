/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.topology;

import com.google.common.annotations.Beta;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * Provides initial and failed state for NodeManagers
 */
@Beta
public interface InitialStateProvider {
    @Nonnull
    Node getInitialState(@Nonnull final NodeId nodeId, @Nonnull final Node configNode);

    @Nonnull
    Node getFailedState(@Nonnull final NodeId nodeId, @Nonnull final Node configNode);
}

