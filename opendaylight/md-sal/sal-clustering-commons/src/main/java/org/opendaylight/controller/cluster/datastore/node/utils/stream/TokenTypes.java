/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

final class TokenTypes {
    private TokenTypes() {
        throw new UnsupportedOperationException();
    }

    static final byte SIGNATURE_MARKER = (byte) 0xab;

    /**
     * Original stream version. Uses a per-stream dictionary for strings. QNames are serialized as three strings.
     */
    static final short LITHIUM_VERSION = 1;

    // Tokens supported in LITHIUM_VERSION
    static final byte IS_CODE_VALUE = 1;
    static final byte IS_STRING_VALUE = 2;
    static final byte IS_NULL_VALUE = 3;
}
