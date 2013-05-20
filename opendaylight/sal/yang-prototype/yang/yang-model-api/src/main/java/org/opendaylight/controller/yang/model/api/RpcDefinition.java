/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api;

import java.util.Set;

/**
 * Interface describing YANG 'rpc' statement.
 * <p>
 * The rpc statement defines an rpc node in the schema tree. Under the rpc node,
 * a schema node with the name 'input', and a schema node with the name 'output'
 * are also defined.
 * </p>
 */
public interface RpcDefinition extends SchemaNode {

    /**
     * @return Set of type definitions declared under this rpc statement.
     */
    Set<TypeDefinition<?>> getTypeDefinitions();

    /**
     * @return Set of grouping statements declared under this rpc statement.
     */
    Set<GroupingDefinition> getGroupings();

    /**
     * @return Definition of input parameters to the RPC operation. The
     *         substatements of input define nodes under the RPC's input node.
     */
    ContainerSchemaNode getInput();

    /**
     * @return Definition of output parameters to the RPC operation. The
     *         substatements of output define nodes under the RPC's output node.
     */
    ContainerSchemaNode getOutput();

}
