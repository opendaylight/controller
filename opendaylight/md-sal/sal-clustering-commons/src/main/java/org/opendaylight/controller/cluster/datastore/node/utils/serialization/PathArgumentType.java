/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import java.util.Map;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import com.google.common.collect.ImmutableMap;

public enum PathArgumentType {
    AUGMENTATION_IDENTIFIER,
    NODE_IDENTIFIER,
    NODE_IDENTIFIER_WITH_VALUE,
    NODE_IDENTIFIER_WITH_PREDICATES;

    private static Map<Class<?>, PathArgumentType> CLASS_TO_ENUM_MAP =
            ImmutableMap.<Class<?>, PathArgumentType>builder().
                put(YangInstanceIdentifier.AugmentationIdentifier.class, AUGMENTATION_IDENTIFIER).
                put(YangInstanceIdentifier.NodeIdentifier.class, NODE_IDENTIFIER).
                put(YangInstanceIdentifier.NodeIdentifierWithPredicates.class, NODE_IDENTIFIER_WITH_PREDICATES).
                put(YangInstanceIdentifier.NodeWithValue.class, NODE_IDENTIFIER_WITH_VALUE).build();

    public static int getSerializablePathArgumentType(YangInstanceIdentifier.PathArgument pathArgument){

        PathArgumentType type = CLASS_TO_ENUM_MAP.get(pathArgument.getClass());
        if(type == null) {
            throw new IllegalArgumentException("Unknown type of PathArgument = " + pathArgument);
        }

        return type.ordinal();
    }

}
