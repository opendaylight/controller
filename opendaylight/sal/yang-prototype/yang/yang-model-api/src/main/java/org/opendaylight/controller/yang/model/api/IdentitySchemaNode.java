/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api;

/**
 * Interface describing YANG 'identity' statement.
 * <p>
 * The 'identity' statement is used to define a new globally unique, abstract,
 * and untyped identity. Its only purpose is to denote its name, semantics, and
 * existence. The built-in datatype "identityref" can be used to reference
 * identities within a data model.
 * </p>
 */
public interface IdentitySchemaNode extends SchemaNode {

    /**
     * @return an existing identity, from which the new identity is derived or
     *         null, if the identity is defined from scratch.
     */
    IdentitySchemaNode getBaseIdentity();

}
