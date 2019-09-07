/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

final class ValueTypes {
    // The String length threshold beyond which a String should be encoded as bytes
    static final int STRING_BYTES_LENGTH_THRESHOLD = Short.MAX_VALUE / 4;

    static final byte SHORT_TYPE = 1;
    static final byte BYTE_TYPE = 2;
    static final byte INT_TYPE = 3;
    static final byte LONG_TYPE = 4;
    static final byte BOOL_TYPE = 5;
    static final byte QNAME_TYPE = 6;
    static final byte BITS_TYPE = 7;
    static final byte YANG_IDENTIFIER_TYPE = 8;
    static final byte STRING_TYPE = 9;
    static final byte BIG_INTEGER_TYPE = 10;
    static final byte BIG_DECIMAL_TYPE = 11;
    static final byte BINARY_TYPE = 12;
    // Leaf nodes no longer allow null values. The "empty" type is now represented as
    // org.opendaylight.yangtools.yang.common.Empty. This is kept for backwards compatibility.
    @Deprecated
    static final byte NULL_TYPE = 13;
    static final byte STRING_BYTES_TYPE = 14;
    static final byte EMPTY_TYPE = 15;

    private ValueTypes() {
        throw new UnsupportedOperationException("Utility class");
    }
}
