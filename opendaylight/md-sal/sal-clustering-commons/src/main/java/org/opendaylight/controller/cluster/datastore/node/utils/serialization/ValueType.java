/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum ValueType {
    SHORT_TYPE,
    BYTE_TYPE,
    INT_TYPE,
    LONG_TYPE,
    BOOL_TYPE,
    QNAME_TYPE,
    BITS_TYPE,
    YANG_IDENTIFIER_TYPE,
    STRING_TYPE,
    BIG_INTEGER_TYPE,
    BIG_DECIMAL_TYPE;

    private static Map<Class, ValueType> types = new HashMap<>();

    static {
        types.put(String.class, STRING_TYPE);
        types.put(Byte.class, BYTE_TYPE);
        types.put(Integer.class, INT_TYPE);
        types.put(Long.class, LONG_TYPE);
        types.put(Boolean.class, BOOL_TYPE);
        types.put(QName.class, QNAME_TYPE);
        types.put(Set.class, BITS_TYPE);
        types.put(YangInstanceIdentifier.class, YANG_IDENTIFIER_TYPE);
        types.put(Short.class,SHORT_TYPE);
        types.put(BigInteger.class, BIG_INTEGER_TYPE);
        types.put(BigDecimal.class, BIG_DECIMAL_TYPE);
    }

    public static final ValueType getSerializableType(Object node){
        Preconditions.checkNotNull(node, "node should not be null");

        if(types.containsKey(node.getClass())) {
            return types.get(node.getClass());
        } else if(node instanceof Set){
            return BITS_TYPE;
        }

        throw new IllegalArgumentException("Unknown value type " + node.getClass().getSimpleName());
    }
}
