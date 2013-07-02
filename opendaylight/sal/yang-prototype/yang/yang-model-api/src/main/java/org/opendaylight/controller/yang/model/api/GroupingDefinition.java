/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api;

/**
 * Interface describing YANG 'grouping' statement.
 * <p>
 * It is used to define a reusable block of nodes, which may be used locally in
 * the module, in modules that include it, and by other modules that import from
 * it.
 * </p>
 */
public interface GroupingDefinition extends DataNodeContainer, SchemaNode {

    /**
     * Returns <code>true</code> if the data node was added by uses statement,
     * otherwise returns <code>false</code>
     *
     * @return <code>true</code> if the data node was added by uses statement,
     *         otherwise returns <code>false</code>
     */
    boolean isAddedByUses();

}
