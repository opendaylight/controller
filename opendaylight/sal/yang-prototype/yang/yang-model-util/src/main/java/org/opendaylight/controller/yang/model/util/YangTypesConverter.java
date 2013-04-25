/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.util;

import java.net.URI;
import java.util.Date;
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
import org.opendaylight.controller.yang.model.api.type.EmptyTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.InstanceIdentifierTypeDefinition;

public class YangTypesConverter {

    private static final Map<String, TypeDefinition<? extends TypeDefinition<?>>> baseYangTypeMap = new HashMap<String, TypeDefinition<? extends TypeDefinition<?>>>();
    private static final Set<String> baseYangTypes = new HashSet<String>();

    private static final TypeDefinition<BinaryTypeDefinition> BINARY = new BinaryType();
    private static final TypeDefinition<BitsTypeDefinition> BITS = new BitsType();
    private static final TypeDefinition<BooleanTypeDefinition> BOOLEAN_TYPE = new BooleanType();
    private static final TypeDefinition<EmptyTypeDefinition> EMPTY_TYPE = new EmptyType();
    private static final TypeDefinition<InstanceIdentifierTypeDefinition> INST_ID_TYPE = new InstanceIdentifier(
            null, true);

    static {
        baseYangTypeMap.put("binary", BINARY);
        baseYangTypeMap.put("bits", BITS);
        baseYangTypeMap.put("boolean", BOOLEAN_TYPE);
        baseYangTypeMap.put("empty", EMPTY_TYPE);
        baseYangTypeMap.put("instance-identifier", INST_ID_TYPE);

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

    public static TypeDefinition<?> javaTypeForBaseYangType(
            List<String> actualPath, URI namespace, Date revision,
            String typeName) {

        if (typeName.startsWith("int")) {
            if (typeName.equals("int8")) {
                return new Int8(actualPath, namespace, revision);
            } else if (typeName.equals("int16")) {
                return new Int16(actualPath, namespace, revision);
            } else if (typeName.equals("int32")) {
                return new Int32(actualPath, namespace, revision);
            } else if (typeName.equals("int64")) {
                return new Int64(actualPath, namespace, revision);
            }
        } else if (typeName.startsWith("uint")) {
            if (typeName.equals("uint8")) {
                return new Uint8(actualPath, namespace, revision);
            } else if (typeName.equals("uint16")) {
                return new Uint16(actualPath, namespace, revision);
            } else if (typeName.equals("uint32")) {
                return new Uint32(actualPath, namespace, revision);
            } else if (typeName.equals("uint64")) {
                return new Uint64(actualPath, namespace, revision);
            }
        } else if (typeName.equals("string")) {
            return new StringType(actualPath, namespace, revision);
        }

        TypeDefinition<?> type = baseYangTypeMap.get(typeName);
        return type;
    }

}
