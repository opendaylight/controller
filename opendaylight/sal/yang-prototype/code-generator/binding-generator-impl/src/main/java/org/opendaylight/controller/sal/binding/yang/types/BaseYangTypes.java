/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.yang.types;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.binding.generator.util.Types;
import org.opendaylight.controller.sal.binding.generator.spi.TypeProvider;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.yang.model.api.TypeDefinition;

public class BaseYangTypes {

    private static Map<String, Type> typeMap = new HashMap<String, Type>();

    public static final Type BOOLEAN_TYPE = Types.typeForClass(Boolean.class);
    public static final Type INT8_TYPE = Types.typeForClass(Byte.class);
    public static final Type INT16_TYPE = Types.typeForClass(Short.class);
    public static final Type INT32_TYPE = Types.typeForClass(Integer.class);
    public static final Type INT64_TYPE = Types.typeForClass(Long.class);
    public static final Type STRING_TYPE = Types.typeForClass(String.class);
    public static final Type DECIMAL64_TYPE = Types.typeForClass(Double.class);
    public static final Type UINT8_TYPE = Types.typeForClass(Short.class);
    public static final Type UINT16_TYPE = Types.typeForClass(Integer.class);
    public static final Type UINT32_TYPE = Types.typeForClass(Long.class);
    public static final Type UINT64_TYPE = Types.typeForClass(BigInteger.class);

    static {
        typeMap.put("boolean", BOOLEAN_TYPE);
        typeMap.put("int8", INT8_TYPE);
        typeMap.put("int16", INT16_TYPE);
        typeMap.put("int32", INT32_TYPE);
        typeMap.put("int64", INT64_TYPE);
        typeMap.put("string", STRING_TYPE);
        typeMap.put("decimal64", DECIMAL64_TYPE);
        typeMap.put("uint8", UINT8_TYPE);
        typeMap.put("uint16", UINT16_TYPE);
        typeMap.put("uint32", UINT32_TYPE);
        typeMap.put("uint64", UINT64_TYPE);
    }

    public static final TypeProvider BASE_YANG_TYPES_PROVIDER = new TypeProvider() {

        @Override
        public Type javaTypeForYangType(String type) {
            return typeMap.get(type);
        }

        @Override
        public Type javaTypeForSchemaDefinitionType(TypeDefinition<?> type) {
            if (type != null) {
                return typeMap.get(type.getQName().getLocalName());
            }

            return null;
        }
    };

}
