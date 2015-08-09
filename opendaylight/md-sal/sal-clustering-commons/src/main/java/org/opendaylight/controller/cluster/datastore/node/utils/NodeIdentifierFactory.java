/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

import java.util.HashMap;
import java.util.Map;

public class NodeIdentifierFactory {
    private static final Map<String, YangInstanceIdentifier.PathArgument> cache = new HashMap<>();
    public static YangInstanceIdentifier.PathArgument getArgument(String id){
        YangInstanceIdentifier.PathArgument value = cache.get(id);
        if(value == null){
            synchronized (cache){
                value = cache.get(id);
                if(value == null) {
                    value = createPathArgument(id, null);
                    cache.put(id, value);
                }
            }
        }
        return value;
    }

    public static YangInstanceIdentifier.PathArgument createPathArgument(String id, DataSchemaNode schemaNode){
        final NodeIdentifierWithPredicatesGenerator
            nodeIdentifierWithPredicatesGenerator = new NodeIdentifierWithPredicatesGenerator(id, schemaNode);
        if(nodeIdentifierWithPredicatesGenerator.matches()){
            return nodeIdentifierWithPredicatesGenerator.getPathArgument();
        }

        final NodeIdentifierWithValueGenerator
            nodeWithValueGenerator = new NodeIdentifierWithValueGenerator(id, schemaNode);
        if(nodeWithValueGenerator.matches()){
            return nodeWithValueGenerator.getPathArgument();
        }

        final AugmentationIdentifierGenerator augmentationIdentifierGenerator = new AugmentationIdentifierGenerator(id);
        if(augmentationIdentifierGenerator.matches()){
            return augmentationIdentifierGenerator.getPathArgument();
        }

        return new NodeIdentifierGenerator(id).getArgument();
    }
}
