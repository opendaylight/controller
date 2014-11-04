/*
 * Copyright (c) 2013 Big Switch Networks, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeTable;
import org.opendaylight.controller.sal.utils.NodeTableCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TableConverter {
    private static final Logger log = LoggerFactory
            .getLogger(TableConverter.class);

    public static NodeTable toNodeTable(byte tableId, Node node) {
        log.trace("Openflow table ID: {}", Byte.toString(tableId));
        return NodeTableCreator.createNodeTable(tableId, node);
    }

    public static byte toOFTable(NodeTable salTable) {
        log.trace("SAL Table: {}", salTable);
        return (Byte) salTable.getID();
    }
}
