/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.topology.util;

import org.opendaylight.controller.netconf.topology.util.NodeWriter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingSalNodeWriter implements NodeWriter {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingSalNodeWriter.class);

    @Override
    public void init(final NodeId id, final Node operationalDataNode) {
        LOG.warn("Init recieved");
        LOG.warn("NodeId: {}", id.getValue());
        LOG.warn("Node: {}", operationalDataNode);
    }

    @Override
    public void update(final NodeId id, final Node operationalDataNode) {
        LOG.warn("Update recieved");
        LOG.warn("NodeId: {}", id.getValue());
        LOG.warn("Node: {}", operationalDataNode);
    }

    @Override
    public void delete(final NodeId id) {
        LOG.warn("Delete recieved");
        LOG.warn("NodeId: {}", id.getValue());
    }
}