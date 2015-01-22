/*
 * Copyright (c) 2013 Big Switch Networks, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.utils;

import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeTable;
import org.opendaylight.controller.sal.core.NodeTable.NodeTableIDType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Deprecated
public class NodeTableCreator {
    protected static final Logger logger = LoggerFactory
            .getLogger(NodeTableCreator.class);

    /**
     * Generic NodeTable creator
     * The nodeTable type is OPENFLOW only for the time being
     *
     * @param portId
     * @param node
     * @return
     */
    public static NodeTable createNodeTable(byte tableId, Node node) {
        try {
            return new NodeTable(NodeTableIDType.OPENFLOW, tableId, node);
        } catch (ConstructionException e1) {
            logger.error("",e1);
            return null;
        }
    }

    public static NodeTable createOFNodeTable(byte tableId, Node node) {
        try {
            return new NodeTable(NodeTableIDType.OPENFLOW, tableId, node);
        } catch (ConstructionException e1) {
            logger.error("",e1);
            return null;
        }
    }

}
