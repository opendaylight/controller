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

public interface EnumTypeDefinition extends TypeDefinition<EnumTypeDefinition> {

    List<EnumPair> getValues();

    interface EnumPair extends SchemaNode {

        /**
         * The name to specify each assigned name of an enumeration type.
         * 
         * @return name of each assigned name of an enumeration type.
         */
        public String getName();

        /**
         * The "value" statement, which is optional, is used to associate an
         * integer value with the assigned name for the enum. This integer value
         * MUST be in the range -2147483648 to 2147483647, and it MUST be unique
         * within the enumeration type.
         * 
         * @return integer value assigned to enumeration
         */
        public Integer getValue();
    }
}
