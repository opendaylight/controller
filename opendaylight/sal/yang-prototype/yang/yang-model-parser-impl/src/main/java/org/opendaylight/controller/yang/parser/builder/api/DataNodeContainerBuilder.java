/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.api;

import java.util.Set;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;

/**
 * Interface for all yang data-node containers [augment, case, container,
 * grouping, list, module, notification].
 */
public interface DataNodeContainerBuilder extends Builder {

    /**
     * Get qname of this node.
     *
     * @return QName of this node
     */
    QName getQName();

    /**
     * Get schema path of this node.
     *
     * @return SchemaPath of this node
     */
    SchemaPath getPath();

    /**
     * Get already built child nodes.
     *
     * @return collection of child nodes
     */
    Set<DataSchemaNode> getChildNodes();

    /**
     * Get builders of child nodes.
     *
     * @return collection child nodes builders
     */
    Set<DataSchemaNodeBuilder> getChildNodeBuilders();

    /**
     * Get child node by name.
     *
     * @param name
     *            name of child to seek
     * @return child node with given name if present, null otherwise
     */
    DataSchemaNodeBuilder getDataChildByName(String name);

    /**
     * Add builder of child node to this node.
     *
     * @param childNode
     */
    void addChildNode(DataSchemaNodeBuilder childNode);

    /**
     * Get already built groupings defined in this node.
     *
     * @return collection of GroupingDefinition objects
     */
    Set<GroupingDefinition> getGroupings();

    /**
     * Get builders of groupings defined in this node.
     *
     * @return collection of grouping builders
     */
    Set<GroupingBuilder> getGroupingBuilders();

    /**
     * Add builder of grouping statement to this node.
     *
     * @param groupingBuilder
     */
    void addGrouping(GroupingBuilder groupingBuilder);

    /**
     * Add builder of uses statement to this node.
     *
     * @param usesBuilder
     */
    void addUsesNode(UsesNodeBuilder usesBuilder);

    /**
     * Get builders of typedef statement defined in this node.
     *
     * @return
     */
    Set<TypeDefinitionBuilder> getTypeDefinitionBuilders();

    /**
     * Add typedef builder to this node.
     *
     * @param typedefBuilder
     */
    void addTypedef(TypeDefinitionBuilder typedefBuilder);

}
