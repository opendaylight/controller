/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.cluster.datastore.node.utils.QNameFactory;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public enum ValueType {
    SHORT_TYPE {
        @Override
        Object deserialize(final String str) {
            return Short.valueOf(str);
        }
    },
    BYTE_TYPE {
        @Override
        Object deserialize(final String str) {
            return Byte.valueOf(str);
        }
    },
    INT_TYPE {
        @Override
        Object deserialize(final String str) {
            return Integer.valueOf(str);
        }
    },
    LONG_TYPE {
        @Override
        Object deserialize(final String str) {
            return Long.valueOf(str);
        }
    },
    BOOL_TYPE {
        @Override
        Object deserialize(final String str) {
            return Boolean.valueOf(str);
        }
    },
    QNAME_TYPE {
        @Override
        Object deserialize(final String str) {
            return QNameFactory.create(str);
        }
    },
    BITS_TYPE {
        @Override
        Object deserialize(final String str) {
            throw new UnsupportedOperationException("Should have been caught by caller");
        }
    },
    YANG_IDENTIFIER_TYPE {
        @Override
        Object deserialize(final String str) {
            throw new UnsupportedOperationException("Should have been caught by caller");
        }
    },
    STRING_TYPE {
        @Override
        Object deserialize(final String str) {
            return str;
        }
    },
    BIG_INTEGER_TYPE {
        @Override
        Object deserialize(final String str) {
            return new BigInteger(str);
        }
    },
    BIG_DECIMAL_TYPE {
        @Override
        Object deserialize(final String str) {
            return new BigDecimal(str);
        }
    },
    BINARY_TYPE {
        @Override
        Object deserialize(final String str) {
            throw new UnsupportedOperationException("Should have been caught by caller");
        }
    };

    private static final Map<Class<?>, ValueType> TYPES;

    static {
        final Builder<Class<?>, ValueType> b = ImmutableMap.builder();

        b.put(String.class, STRING_TYPE);
        b.put(Byte.class, BYTE_TYPE);
        b.put(Integer.class, INT_TYPE);
        b.put(Long.class, LONG_TYPE);
        b.put(Boolean.class, BOOL_TYPE);
        b.put(QName.class, QNAME_TYPE);
        b.put(YangInstanceIdentifier.class, YANG_IDENTIFIER_TYPE);
        b.put(Short.class,SHORT_TYPE);
        b.put(BigInteger.class, BIG_INTEGER_TYPE);
        b.put(BigDecimal.class, BIG_DECIMAL_TYPE);
        b.put(byte[].class, BINARY_TYPE);

        TYPES = b.build();
    }

    abstract Object deserialize(String str);

    public static final ValueType getSerializableType(Object node) {
        Preconditions.checkNotNull(node, "node should not be null");

        final ValueType type = TYPES.get(node.getClass());
        if (type != null) {
            return type;
        }
        if (node instanceof Set) {
            return BITS_TYPE;
        }

        throw new IllegalArgumentException("Unknown value type " + node.getClass().getSimpleName());
    }
}
