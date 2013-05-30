/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.TypeDefinition;

public class YangTypesConverter {
    private static final Set<String> baseYangTypes = new HashSet<String>();

    static {
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

    public static TypeDefinition<?> javaTypeForBaseYangType(
            List<String> actualPath, URI namespace, Date revision,
            String typeName) {
        TypeDefinition<?> type = null;

        SchemaPath path = createSchemaPath(actualPath, namespace, revision);
        if (typeName.startsWith("int")) {
            if (typeName.equals("int8")) {
                type = new Int8(path);
            } else if (typeName.equals("int16")) {
                type = new Int16(path);
            } else if (typeName.equals("int32")) {
                type = new Int32(path);
            } else if (typeName.equals("int64")) {
                type = new Int64(path);
            }
        } else if (typeName.startsWith("uint")) {
            if (typeName.equals("uint8")) {
                type = new Uint8(path);
            } else if (typeName.equals("uint16")) {
                type = new Uint16(path);
            } else if (typeName.equals("uint32")) {
                type = new Uint32(path);
            } else if (typeName.equals("uint64")) {
                type = new Uint64(path);
            }
        } else if ("string".equals(typeName)) {
            type = new StringType(path);
        } else if("binary".equals(typeName)) {
            type = new BinaryType(path);
        } else if("boolean".equals(typeName)) {
            type = new BooleanType(path);
        } else if("empty".equals(typeName)) {
            type = new EmptyType(path);
        } else if("instance-identifier".equals(typeName)) {
            type = new InstanceIdentifier(path, null, true);
        }

        return type;
    }

    private static SchemaPath createSchemaPath(List<String> actualPath, URI namespace, Date revision) {
        List<QName> path = new ArrayList<QName>();
        for(String element : actualPath) {
            path.add(new QName(namespace, revision, element));
        }
        return new SchemaPath(path, true);
    }

}
