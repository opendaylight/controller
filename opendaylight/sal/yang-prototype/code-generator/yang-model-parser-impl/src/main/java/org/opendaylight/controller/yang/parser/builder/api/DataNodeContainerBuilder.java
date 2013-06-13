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
import org.opendaylight.controller.yang.model.api.SchemaPath;

/**
 * Interface for all yang data-node containers [augment, case, container,
 * grouping, list, module, notification].
 */
public interface DataNodeContainerBuilder extends Builder {

    QName getQName();

    Set<DataSchemaNodeBuilder> getChildNodes();

    void addChildNode(DataSchemaNodeBuilder childNode);

    Set<GroupingBuilder> getGroupings();

    void addGrouping(GroupingBuilder groupingBuilder);

    void addUsesNode(UsesNodeBuilder usesBuilder);

    Set<TypeDefinitionBuilder> getTypeDefinitions();

    void addTypedef(TypeDefinitionBuilder typedefBuilder);

    SchemaPath getPath();

}
