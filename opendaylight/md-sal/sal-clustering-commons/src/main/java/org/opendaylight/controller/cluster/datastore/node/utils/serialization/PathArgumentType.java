/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public enum PathArgumentType {
    AUGMENTATION_IDENTIFIER,
    NODE_IDENTIFIER,
    NODE_IDENTIFIER_WITH_VALUE,
    NODE_IDENTIFIER_WITH_PREDICATES;

    public static int getSerializablePathArgumentType(YangInstanceIdentifier.PathArgument pathArgument){
        Preconditions.checkNotNull(pathArgument, "pathArgument should not be null");

        if(pathArgument instanceof YangInstanceIdentifier.AugmentationIdentifier){
            return AUGMENTATION_IDENTIFIER.ordinal();
        } else if(pathArgument instanceof YangInstanceIdentifier.NodeIdentifier){
            return NODE_IDENTIFIER.ordinal();
        } else if(pathArgument instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates){
            return NODE_IDENTIFIER_WITH_PREDICATES.ordinal();
        } else if(pathArgument instanceof YangInstanceIdentifier.NodeWithValue){
            return NODE_IDENTIFIER_WITH_VALUE.ordinal();
        }
        throw new IllegalArgumentException("Unknown type of PathArgument = " + pathArgument.toString());
    }

}
