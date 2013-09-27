/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.attribute;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;

import javax.management.openmbean.SimpleType;

import org.opendaylight.yangtools.sal.binding.model.api.Type;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class SimpleTypeResolver {

    public static SimpleType<?> getSimpleType(Type type) {
        SimpleType<?> expectedSimpleType = JAVA_TYPE_TO_SIMPLE_TYPE.get(type
                .getFullyQualifiedName());
        Preconditions.checkState(expectedSimpleType != null,
                "Cannot find simple type for " + type.getFullyQualifiedName());
        return expectedSimpleType;
    }

    public static SimpleType<?> getSimpleType(String fullyQualifiedName) {
        SimpleType<?> expectedSimpleType = JAVA_TYPE_TO_SIMPLE_TYPE
                .get(fullyQualifiedName);
        Preconditions.checkState(expectedSimpleType != null,
                "Cannot find simple type for " + fullyQualifiedName);
        return expectedSimpleType;
    }

    private static final Map<String, SimpleType<?>> JAVA_TYPE_TO_SIMPLE_TYPE = Maps
            .newHashMap();
    static {
        // TODO add all
        JAVA_TYPE_TO_SIMPLE_TYPE.put(Integer.class.getName(),
                SimpleType.INTEGER);
        JAVA_TYPE_TO_SIMPLE_TYPE.put(int.class.getName(), SimpleType.INTEGER);
        JAVA_TYPE_TO_SIMPLE_TYPE.put(Short.class.getName(), SimpleType.SHORT);
        JAVA_TYPE_TO_SIMPLE_TYPE.put(short.class.getName(), SimpleType.SHORT);
        JAVA_TYPE_TO_SIMPLE_TYPE.put(Long.class.getName(), SimpleType.LONG);
        JAVA_TYPE_TO_SIMPLE_TYPE.put(long.class.getName(), SimpleType.LONG);
        JAVA_TYPE_TO_SIMPLE_TYPE.put(String.class.getName(), SimpleType.STRING);
        JAVA_TYPE_TO_SIMPLE_TYPE.put(Boolean.class.getName(),
                SimpleType.BOOLEAN);
        JAVA_TYPE_TO_SIMPLE_TYPE.put(boolean.class.getName(),
                SimpleType.BOOLEAN);
        JAVA_TYPE_TO_SIMPLE_TYPE.put(BigInteger.class.getName(),
                SimpleType.BIGINTEGER);
        JAVA_TYPE_TO_SIMPLE_TYPE.put(BigDecimal.class.getName(),
                SimpleType.BIGDECIMAL);
        JAVA_TYPE_TO_SIMPLE_TYPE.put(Byte.class.getName(), SimpleType.BYTE);
        JAVA_TYPE_TO_SIMPLE_TYPE.put(byte.class.getName(), SimpleType.BYTE);
        JAVA_TYPE_TO_SIMPLE_TYPE.put(Date.class.getName(), SimpleType.DATE);
        JAVA_TYPE_TO_SIMPLE_TYPE.put(Double.class.getName(), SimpleType.DOUBLE);
        JAVA_TYPE_TO_SIMPLE_TYPE.put(double.class.getName(), SimpleType.DOUBLE);
    }

}
