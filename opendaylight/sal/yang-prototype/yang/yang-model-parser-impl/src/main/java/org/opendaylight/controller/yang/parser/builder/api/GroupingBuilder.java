/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.api;

import java.util.Set;

import org.opendaylight.controller.yang.model.api.GroupingDefinition;

/**
 * Interface for builders of 'grouping' statement.
 */
public interface GroupingBuilder extends DataNodeContainerBuilder, SchemaNodeBuilder, GroupingMember {

    /**
     * Build GroupingDefinition object from this builder.
     */
    GroupingDefinition build();

    /**
     * Get uses statement defined in this builder
     *
     * @return collection of builders of uses statements
     */
    Set<UsesNodeBuilder> getUses();

}
