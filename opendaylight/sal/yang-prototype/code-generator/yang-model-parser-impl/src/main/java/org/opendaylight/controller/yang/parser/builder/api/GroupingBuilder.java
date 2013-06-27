/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.api;

import java.util.List;
import java.util.Set;

import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.parser.builder.impl.UnknownSchemaNodeBuilder;

/**
 * Interface for builders of 'grouping' statement.
 */
public interface GroupingBuilder extends DataNodeContainerBuilder, SchemaNodeBuilder, TypeDefinitionAwareBuilder, GroupingMember {

    GroupingDefinition build();

    DataSchemaNodeBuilder getChildNode(String name);

    List<UnknownSchemaNodeBuilder> getUnknownNodes();

    Set<UsesNodeBuilder> getUses();

}
