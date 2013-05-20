/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api;

/**
 * Interface describing YANG 'leaf-list' statement.
 */
public interface LeafListSchemaNode extends DataSchemaNode {

    TypeDefinition<? extends TypeDefinition<?>> getType();

    /**
     * YANG 'ordered-by' statement. It defines whether the order of entries
     * within this leaf-list are determined by the user or the system. If not
     * present, default is false.
     *
     * @return true if ordered-by argument is "user", false otherwise
     */
    boolean isUserOrdered();

}
