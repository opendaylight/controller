/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ValueType {
    public static Integer SHORT_TYPE = new Integer(0);
    public static Integer BYTE_TYPE = new Integer(1);
    public static Integer INT_TYPE = new Integer(2);
    public static Integer LONG_TYPE = new Integer(3);
    public static Integer BOOL_TYPE = new Integer(4);
    public static Integer QNAME_TYPE = new Integer(5);
    public static Integer BITS_TYPE = new Integer(6);
    public static Integer YANG_IDENTIFIER_TYPE = new Integer(7);
    public static Integer STRING_TYPE = new Integer(8);
    public static Integer BIG_INTEGER_TYPE = new Integer(9);
    public static Integer BIG_DECIMAL_TYPE = new Integer(10);

    private static Map<Class, Integer> types = new HashMap<>();

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

    public static Integer getSerializableType(Object node){
        if(types.containsKey(node.getClass())) {
            return types.get(node.getClass());
        } else if(node instanceof Set){
            return BITS_TYPE;
        }

        throw new IllegalArgumentException("Unknown value type " + node.getClass().getSimpleName());
    }
}
