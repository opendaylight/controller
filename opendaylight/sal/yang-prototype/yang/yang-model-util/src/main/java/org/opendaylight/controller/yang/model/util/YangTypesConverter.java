/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.type.BinaryTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.BitsTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.BooleanTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.DecimalTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.InstanceIdentifierTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.IntegerTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;
import org.opendaylight.controller.yang.model.api.type.StringTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.UnsignedIntegerTypeDefinition;

public class YangTypesConverter {

    private static final Map<String, TypeDefinition<? extends TypeDefinition<?>>> baseYangTypeMap = new HashMap<String, TypeDefinition<? extends TypeDefinition<?>>>();
    private static final Set<String> baseYangTypes = new HashSet<String>();

    private static final TypeDefinition<BinaryTypeDefinition> BINARY = new BinaryType();
    private static final TypeDefinition<BitsTypeDefinition> BITS = new BitsType();
    private static final TypeDefinition<BooleanTypeDefinition> BOOLEAN_TYPE = new BooleanType();
    private static final TypeDefinition<InstanceIdentifierTypeDefinition> INST_ID_TYPE = new InstanceIdentifier(null, true);
    private static final TypeDefinition<IntegerTypeDefinition> INT8_TYPE = new Int8();
    private static final TypeDefinition<IntegerTypeDefinition> INT16_TYPE = new Int16();
    private static final TypeDefinition<IntegerTypeDefinition> INT32_TYPE = new Int32();
    private static final TypeDefinition<IntegerTypeDefinition> INT64_TYPE = new Int64();
    private static final TypeDefinition<StringTypeDefinition> STRING_TYPE = new StringType();
    private static final TypeDefinition<UnsignedIntegerTypeDefinition> UINT8_TYPE = new Uint8();
    private static final TypeDefinition<UnsignedIntegerTypeDefinition> UINT16_TYPE = new Uint16();
    private static final TypeDefinition<UnsignedIntegerTypeDefinition> UINT32_TYPE = new Uint32();
    private static final TypeDefinition<UnsignedIntegerTypeDefinition> UINT64_TYPE = new Uint64();

    static {
        baseYangTypeMap.put("binary", BINARY);
        baseYangTypeMap.put("bits", BITS);
        baseYangTypeMap.put("boolean", BOOLEAN_TYPE);
        baseYangTypeMap.put("instance-identifier", INST_ID_TYPE);
        baseYangTypeMap.put("int8", INT8_TYPE);
        baseYangTypeMap.put("int16", INT16_TYPE);
        baseYangTypeMap.put("int32", INT32_TYPE);
        baseYangTypeMap.put("int64", INT64_TYPE);
        baseYangTypeMap.put("string", STRING_TYPE);
        baseYangTypeMap.put("uint8", UINT8_TYPE);
        baseYangTypeMap.put("uint16", UINT16_TYPE);
        baseYangTypeMap.put("uint32", UINT32_TYPE);
        baseYangTypeMap.put("uint64", UINT64_TYPE);

        baseYangTypes.add("binary");
        baseYangTypes.add("bits");
        baseYangTypes.add("boolean");
        baseYangTypes.add("decimal64");
        baseYangTypes.add("empty");
        baseYangTypes.add("enumeration");
        baseYangTypes.add("identityref");
        baseYangTypes.add("instance-identifier");
        baseYangTypes.add("int8");
        baseYangTypes.add("int16");
        baseYangTypes.add("int32");
        baseYangTypes.add("int64");
        baseYangTypes.add("leafref");
        baseYangTypes.add("string");
        baseYangTypes.add("uint8");
        baseYangTypes.add("uint16");
        baseYangTypes.add("uint32");
        baseYangTypes.add("uint64");
        baseYangTypes.add("union");
    }

    public static boolean isBaseYangType(String type) {
        return baseYangTypes.contains(type);
    }

    public static TypeDefinition<?> javaTypeForBaseYangType(QName typeQName) {
        TypeDefinition<?> type = baseYangTypeMap.get(typeQName.getLocalName());
        return type;
    }

    public static TypeDefinition<?> javaTypeForBaseYangType(String typeName) {
        TypeDefinition<?> type = baseYangTypeMap.get(typeName);
        return type;
    }

    public static TypeDefinition<IntegerTypeDefinition> javaTypeForBaseYangSignedIntegerType(
            String typeName, List<RangeConstraint> ranges) {
        if (typeName.equals("int8")) {
            return new Int8(ranges, null, null);
        } else if (typeName.equals("int16")) {
            return new Int16(ranges, null, null);
        } else if (typeName.equals("int32")) {
            return new Int32(ranges, null, null);
        } else if (typeName.equals("int64")) {
            return new Int64(ranges, null, null);
        }
        return null;
    }

    public static TypeDefinition<UnsignedIntegerTypeDefinition> javaTypeForBaseYangUnsignedIntegerType(
            final String typeName, List<RangeConstraint> ranges) {
        if (typeName.equals("uint8")) {
            return new Uint8(ranges, null, null);
        } else if (typeName.equals("uint16")) {
            return new Uint16(ranges, null, null);
        } else if (typeName.equals("uint32")) {
            return new Uint32(ranges, null, null);
        } else if (typeName.equals("uint64")) {
            return new Uint64(ranges, null, null);
        }
        return null;
    }

    public static TypeDefinition<DecimalTypeDefinition> javaTypeForBaseYangDecimal64Type(
            List<RangeConstraint> rangeStatements, int fractionDigits) {
        return new Decimal64(rangeStatements, fractionDigits);
    }

}
