/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.yang.types;

import org.opendaylight.controller.sal.binding.generator.spi.TypeProvider;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.yang.model.api.TypeDefinition;

public class TypeProviderImpl implements TypeProvider {

    /*
     * (non-Javadoc)
     * 
     * @see org.opendaylight.controller.yang.model.type.provider.TypeProvider#
     * javaTypeForYangType(java.lang.String)
     */
    @Override
    public Type javaTypeForYangType(String type) {
        Type t = BaseYangTypes.BASE_YANG_TYPES_PROVIDER
                .javaTypeForYangType(type);
        // TODO: this needs to be implemented in better way
        // if(t == null) {
        // t = BaseYangTypes.IETF_INET_TYPES_PROVIDER.javaTypeForYangType(type);
        // }
        return t;
    }

    @Override
    public Type javaTypeForSchemaDefinitionType(final TypeDefinition<?> type) {
        if (type != null) {
            Type t = BaseYangTypes.BASE_YANG_TYPES_PROVIDER
                    .javaTypeForSchemaDefinitionType(type);

            if (t != null) {
                return t;
            }
        }
        return null;
    }
}
