/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

final class PathArgumentTypes {
    public static final byte AUGMENTATION_IDENTIFIER = 1;
    public static final byte NODE_IDENTIFIER = 2;
    public static final byte NODE_IDENTIFIER_WITH_VALUE = 3;
    public static final byte NODE_IDENTIFIER_WITH_PREDICATES = 4;

    private PathArgumentTypes() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final Map<Class<?>, Byte> CLASS_TO_ENUM_MAP =
            ImmutableMap.<Class<?>, Byte>builder().
                put(YangInstanceIdentifier.AugmentationIdentifier.class, AUGMENTATION_IDENTIFIER).
                put(YangInstanceIdentifier.NodeIdentifier.class, NODE_IDENTIFIER).
                put(YangInstanceIdentifier.NodeIdentifierWithPredicates.class, NODE_IDENTIFIER_WITH_PREDICATES).
                put(YangInstanceIdentifier.NodeWithValue.class, NODE_IDENTIFIER_WITH_VALUE).build();

    public static byte getSerializablePathArgumentType(YangInstanceIdentifier.PathArgument pathArgument) {
        final Byte type = CLASS_TO_ENUM_MAP.get(pathArgument.getClass());
        Preconditions.checkArgument(type != null, "Unknown type of PathArgument = %s", pathArgument);
        return type;
    }

}
