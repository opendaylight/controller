/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api;

import java.util.List;

import org.opendaylight.controller.yang.common.QName;

/**
 * SchemaNode represents a node in schema tree.
 */
public interface SchemaNode {

    public QName getQName();

    public SchemaPath getPath();

    /**
     * @return textual description of this node.
     */
    public String getDescription();

    /**
     * @return textual cross-reference to an external document that provides
     *         additional information relevant to this node.
     */
    public String getReference();

    /**
     * @return actual status of this node.
     */
    public Status getStatus();

    /**
     * @return collection of all unknown nodes defined under this schema node.
     */
    public List<UnknownSchemaNode> getUnknownSchemaNodes();

}
