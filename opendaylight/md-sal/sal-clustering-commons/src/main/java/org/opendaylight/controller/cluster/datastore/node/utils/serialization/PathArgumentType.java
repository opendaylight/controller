/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class PathArgumentType {
    public static String AUGMENTATION_IDENTIFIER = new Integer(0).toString();
    public static String NODE_IDENTIFIER = new Integer(1).toString();
    public static String NODE_IDENTIFIER_WITH_VALUE = new Integer(2).toString();
    public static String NODE_IDENTIFIER_WITH_PREDICATES = new Integer(3).toString();

    public static String getSerializablePathArgumentType(YangInstanceIdentifier.PathArgument pathArgument){
        if(pathArgument instanceof YangInstanceIdentifier.AugmentationIdentifier){
            return AUGMENTATION_IDENTIFIER;
        } else if(pathArgument instanceof YangInstanceIdentifier.NodeIdentifier){
            return NODE_IDENTIFIER;
        } else if(pathArgument instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates){
            return NODE_IDENTIFIER_WITH_PREDICATES;
        } else if(pathArgument instanceof YangInstanceIdentifier.NodeWithValue){
            return NODE_IDENTIFIER_WITH_VALUE;
        }
        throw new IllegalArgumentException("Unknown type of PathArgument = " + pathArgument.toString());
    }

}
