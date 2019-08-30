/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

final class LithiumPathArgument {
    static final byte AUGMENTATION_IDENTIFIER = 1;
    static final byte NODE_IDENTIFIER = 2;
    static final byte NODE_IDENTIFIER_WITH_VALUE = 3;
    static final byte NODE_IDENTIFIER_WITH_PREDICATES = 4;

    private LithiumPathArgument() {
        throw new UnsupportedOperationException("Utility class");
    }

    static byte getSerializablePathArgumentType(final PathArgument pathArgument) {
        if (pathArgument instanceof NodeIdentifier) {
            return NODE_IDENTIFIER;
        } else if (pathArgument instanceof NodeIdentifierWithPredicates) {
            return NODE_IDENTIFIER_WITH_PREDICATES;
        } else if (pathArgument instanceof AugmentationIdentifier) {
            return AUGMENTATION_IDENTIFIER;
        } else if (pathArgument instanceof NodeWithValue) {
            return NODE_IDENTIFIER_WITH_VALUE;
        } else {
            throw new IllegalArgumentException("Unknown type of PathArgument = " + pathArgument);
        }
    }
}
