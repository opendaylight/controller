/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.topology.util;

import com.google.common.annotations.Beta;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * Customizable code that gets executed after result aggregation, meant for custom writes
 * into the datastore, but any user code can be run here if desired.
 */
@Beta
public interface NodeWriter {

    void init(@Nonnull final NodeId id, @Nonnull final Node operationalDataNode);

    void update(@Nonnull final NodeId id, @Nonnull final Node operationalDataNode);

    void delete(@Nonnull final NodeId id);

}

