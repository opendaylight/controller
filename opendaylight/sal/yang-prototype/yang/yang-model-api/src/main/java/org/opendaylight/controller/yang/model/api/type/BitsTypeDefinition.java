/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api.type;

import java.util.List;

import org.opendaylight.controller.yang.model.api.SchemaNode;
import org.opendaylight.controller.yang.model.api.TypeDefinition;

public interface BitsTypeDefinition extends TypeDefinition<BitsTypeDefinition> {

    public List<Bit> getBits();

    interface Bit extends SchemaNode {
        /**
         * The position value MUST be in the range 0 to 4294967295, and it MUST
         * be unique within the bits type.
         * 
         * @return The position value of bit in range from 0 to 4294967295.
         */
        public Long getPosition();

        public String getName();
    }
}
