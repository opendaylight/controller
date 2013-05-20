/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api;

import java.util.Set;

import org.opendaylight.controller.yang.common.QName;

/**
 * Node which can contains other nodes.
 */
public interface DataNodeContainer {

    /**
     * @return Set of all newly defined types within this DataNodeContainer
     */
    Set<TypeDefinition<?>> getTypeDefinitions();

    /**
     * Set of all child nodes defined within this DataNodeContainer
     */
    Set<DataSchemaNode> getChildNodes();

    /**
     * Set of all groupings defined within this DataNodeContainer
     */
    Set<GroupingDefinition> getGroupings();

    /**
     * @param name
     *            QName of seeked child
     * @return child node of this DataNodeContainer if child with given name is
     *         present, null otherwise
     */
    DataSchemaNode getDataChildByName(QName name);

    /**
     * @param name
     *            name of seeked child as String
     * @return child node of this DataNodeContainer if child with given name is
     *         present, null otherwise
     */
    DataSchemaNode getDataChildByName(String name);

    /**
     * @return Set of all uses nodes defined within this DataNodeContainer
     */
    Set<UsesNode> getUses();

}
