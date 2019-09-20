/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

/**
 * Tokens related to Lithium/NeonSR2 encoding.
 */
final class LithiumTokens {
    /**
     * The value is a reference to a previously-defined entity, typically through {@link #IS_STRING_VALUE}.
     */
    static final byte IS_CODE_VALUE = 1;
    /**
     * The value is a String, which needs to be kept memoized for the purposes for being referenced by
     * {@link #IS_CODE_VALUE}.
     */
    static final byte IS_STRING_VALUE = 2;
    /**
     * The value is an explicit null.
     */
    static final byte IS_NULL_VALUE = 3;

    private LithiumTokens() {

    }
}
