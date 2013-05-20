/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api;

/**
 * Interface describing YANG 'leaf' statement.
 * <p>
 * The 'leaf' statement is used to define a leaf node in the schema tree.
 * </p>
 */
public interface LeafSchemaNode extends DataSchemaNode {

    /**
     * @return type of this leaf
     */
    TypeDefinition<?> getType();

}
