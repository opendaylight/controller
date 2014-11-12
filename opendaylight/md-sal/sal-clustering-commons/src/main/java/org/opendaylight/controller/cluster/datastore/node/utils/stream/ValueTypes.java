/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.base.Preconditions;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class ValueTypes {
    public static final byte SHORT_TYPE = 1;
    public static final byte BYTE_TYPE = 2;
    public static final byte INT_TYPE = 3;
    public static final byte LONG_TYPE = 4;
    public static final byte BOOL_TYPE = 5;
    public static final byte QNAME_TYPE = 6;
    public static final byte BITS_TYPE = 7;
    public static final byte YANG_IDENTIFIER_TYPE = 8;
    public static final byte STRING_TYPE = 9;
    public static final byte BIG_INTEGER_TYPE = 10;
    public static final byte BIG_DECIMAL_TYPE = 11;
    public static final byte BINARY_TYPE = 12;

    private static Map<Class<?>, Byte> types = new HashMap<>();

    static {
        types.put(String.class, Byte.valueOf(STRING_TYPE));
        types.put(Byte.class, Byte.valueOf(BYTE_TYPE));
        types.put(Integer.class, Byte.valueOf(INT_TYPE));
        types.put(Long.class, Byte.valueOf(LONG_TYPE));
        types.put(Boolean.class, Byte.valueOf(BOOL_TYPE));
        types.put(QName.class, Byte.valueOf(QNAME_TYPE));
        types.put(Set.class, Byte.valueOf(BITS_TYPE));
        types.put(YangInstanceIdentifier.class, Byte.valueOf(YANG_IDENTIFIER_TYPE));
        types.put(Short.class, Byte.valueOf(SHORT_TYPE));
        types.put(BigInteger.class, Byte.valueOf(BIG_INTEGER_TYPE));
        types.put(BigDecimal.class, Byte.valueOf(BIG_DECIMAL_TYPE));
        types.put(byte[].class, Byte.valueOf(BINARY_TYPE));
    }

    public static final byte getSerializableType(Object node){
        Preconditions.checkNotNull(node, "node should not be null");

        Byte type = types.get(node.getClass());
        if(type != null) {
            return type;
        } else if(node instanceof Set){
            return BITS_TYPE;
        }

        throw new IllegalArgumentException("Unknown value type " + node.getClass().getSimpleName());
    }
}
