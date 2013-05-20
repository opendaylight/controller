/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api;

/**
 * AugmentationSchema represents augment definition. The "augment" statement
 * allows a module or submodule to add to the schema tree defined in an external
 * module, or the current module and its submodules, and to add to the nodes
 * from a grouping in a "uses" statement.
 */
public interface AugmentationSchema extends DataNodeContainer {

    /**
     * @return when statement
     */
    RevisionAwareXPath getWhenCondition();

    /**
     * @return textual description of this augment.
     */
    String getDescription();

    /**
     * @return textual cross-reference to an external document that provides
     *         additional information relevant to this node.
     */
    String getReference();

    /**
     * @return actual status of this node.
     */
    Status getStatus();

    /**
     * @return SchemaPath that identifies a node in the schema tree. This node
     *         is called the augment's target node. The target node MUST be
     *         either a container, list, choice, case, input, output, or
     *         notification node. It is augmented with the nodes defined as
     *         child nodes of this AugmentationSchema.
     */
    SchemaPath getTargetPath();

}
