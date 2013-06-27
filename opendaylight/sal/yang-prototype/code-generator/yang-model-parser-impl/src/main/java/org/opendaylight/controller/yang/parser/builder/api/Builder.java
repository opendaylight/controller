/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.api;

import org.opendaylight.controller.yang.parser.builder.impl.UnknownSchemaNodeBuilder;

/**
 * Parent interface for all builder interfaces.
 */
public interface Builder {

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

    int getLine();

    void addUnknownSchemaNode(UnknownSchemaNodeBuilder unknownNode);

}
