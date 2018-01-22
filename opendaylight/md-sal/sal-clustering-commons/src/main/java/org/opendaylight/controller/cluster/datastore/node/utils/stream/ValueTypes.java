/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

final class ValueTypes {
    // The String length threshold beyond which a String should be encoded as bytes
    public static final int STRING_BYTES_LENGTH_THRESHOLD = Short.MAX_VALUE / 4;

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
    // Leaf nodes no longer allow null values. The "empty" type is now represented as
    // org.opendaylight.yangtools.yang.common.Empty. This is kept for backwards compatibility.
    @Deprecated
    public static final byte NULL_TYPE = 13;
    public static final byte STRING_BYTES_TYPE = 14;
    public static final byte EMPTY_TYPE = 15;

    private static final Map<Class<?>, Byte> TYPES;

    static {
        final Builder<Class<?>, Byte> b = ImmutableMap.builder();

        b.put(String.class, STRING_TYPE);
        b.put(Byte.class, BYTE_TYPE);
        b.put(Integer.class, INT_TYPE);
        b.put(Long.class, LONG_TYPE);
        b.put(Boolean.class, BOOL_TYPE);
        b.put(QName.class, QNAME_TYPE);
        b.put(Short.class, SHORT_TYPE);
        b.put(BigInteger.class, BIG_INTEGER_TYPE);
        b.put(BigDecimal.class, BIG_DECIMAL_TYPE);
        b.put(byte[].class, BINARY_TYPE);
        b.put(Empty.class, EMPTY_TYPE);

        TYPES = b.build();
    }

    private ValueTypes() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static byte getSerializableType(Object node) {
        Objects.requireNonNull(node);

        final Byte type = TYPES.get(node.getClass());
        if (type != null) {
            if (type == STRING_TYPE && ((String) node).length() >= STRING_BYTES_LENGTH_THRESHOLD) {
                return STRING_BYTES_TYPE;
            }
            return type;
        }

        if (node instanceof Set) {
            return BITS_TYPE;
        }

        if (node instanceof YangInstanceIdentifier) {
            return YANG_IDENTIFIER_TYPE;
        }

        throw new IllegalArgumentException("Unknown value type " + node.getClass().getSimpleName());
    }
}
