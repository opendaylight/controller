/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

/**
 * Tokens used in Neon SR2 encoding. Note that Neon SR2 builds on top of Lithium, hence the token values must never
 * overlap.
 */
final class NeonSR2Tokens {
    static final byte IS_QNAME_CODE = 4;
    static final byte IS_QNAME_VALUE = 5;
    static final byte IS_AUGMENT_CODE = 6;
    static final byte IS_AUGMENT_VALUE = 7;
    static final byte IS_MODULE_CODE = 8;
    static final byte IS_MODULE_VALUE = 9;

    private NeonSR2Tokens() {

    }
}
