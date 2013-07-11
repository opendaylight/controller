/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.api;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.TypeDefinition;

/**
 * Builders of all nodes, which can have 'type' statement must implement this
 * interface. [typedef, type, leaf, leaf-list, deviate]
 */
public interface TypeAwareBuilder extends Builder {

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
     * Get resolved type of this node.
     *
     * @return type of this node if it is already resolved, null otherwise
     */
    TypeDefinition<?> getType();

    /**
     * Get builder of type of this node.
     *
     * @return builder of type of this node or null of this builder has already
     *         resolved type
     */
    TypeDefinitionBuilder getTypedef();

    /**
     * Set resolved type to this node.
     *
     * @param type
     *            type to set
     */
    void setType(TypeDefinition<?> type);

    /**
     * Set builder of type to this node.
     *
     * @param typedef
     *            builder of type to set
     */
    void setTypedef(TypeDefinitionBuilder typedef);

}
