/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api.type;

import java.util.List;

import org.opendaylight.controller.yang.model.api.TypeDefinition;

/**
 * The binary built-in type represents any binary data, i.e., a sequence of
 * octets. <br>
 * <br>
 * Binary values are encoded with the base64 encoding scheme (see <a
 * href="https://tools.ietf.org/html/rfc4648#section-4">[RFC4648], Section
 * 4</a>). <br>
 * The canonical form of a binary value follows the rules in <a
 * href="https://tools.ietf.org/html/rfc4648">[RFC4648]</a>.
 * 
 * <br>
 * <br>
 * This interface was modeled according to definition in <a
 * href="https://tools.ietf.org/html/rfc6020#section-9.8">[RFC-6020] The binary
 * Built-In Type</a>
 * 
 */
public interface BinaryTypeDefinition extends
        TypeDefinition<BinaryTypeDefinition> {

    /**
     * Returns List of number of octets that binary value contains.
     * 
     * @return List of number of octets that binary value contains.
     * 
     * @see LengthConstraint
     */
    public List<LengthConstraint> getLengthConstraints();
}
