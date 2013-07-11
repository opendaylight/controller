/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.api;

import java.util.List;

import org.opendaylight.controller.yang.parser.builder.impl.UnknownSchemaNodeBuilder;

/**
 * Parent interface for all builder interfaces.
 */
public interface Builder {

    /**
     * Get current line in yang file.
     *
     * @return current line in yang file
     */
    int getLine();

    /**
     * Get parent node of this node.
     *
     * @return parent node builder or null if this is top level node
     */
    Builder getParent();

    /**
     * Set parent of this node.
     *
     * @param parent
     *            parent node builder
     */
    void setParent(Builder parent);

    /**
     * Add unknown node to this builder.
     *
     * @param unknownNode
     */
    void addUnknownNodeBuilder(UnknownSchemaNodeBuilder unknownNode);

    /**
     * Get builders of unknown nodes defined in this node.
     *
     * @return collection of UnknownSchemaNodeBuilder objects
     */
    List<UnknownSchemaNodeBuilder> getUnknownNodeBuilders();

    /**
     * Build YANG data model node.
     *
     * This method should create an instance of YANG data model node. After
     * creating an instance, this instance should be returned for each call
     * without repeating build process.
     *
     * @return YANG data model node
     */
    Object build();

}
