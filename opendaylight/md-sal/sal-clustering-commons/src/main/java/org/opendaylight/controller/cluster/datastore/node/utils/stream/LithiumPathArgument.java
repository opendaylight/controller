/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
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

    private static final Map<Class<?>, Byte> CLASS_TO_ENUM_MAP = ImmutableMap.<Class<?>, Byte>builder()
            .put(AugmentationIdentifier.class, AUGMENTATION_IDENTIFIER)
            .put(NodeIdentifier.class, NODE_IDENTIFIER)
            .put(NodeIdentifierWithPredicates.class, NODE_IDENTIFIER_WITH_PREDICATES)
            .put(NodeWithValue.class, NODE_IDENTIFIER_WITH_VALUE).build();

    static byte getSerializablePathArgumentType(final PathArgument pathArgument) {
        final Byte type = CLASS_TO_ENUM_MAP.get(pathArgument.getClass());
        checkArgument(type != null, "Unknown type of PathArgument = %s", pathArgument);
        return type;
    }
}
