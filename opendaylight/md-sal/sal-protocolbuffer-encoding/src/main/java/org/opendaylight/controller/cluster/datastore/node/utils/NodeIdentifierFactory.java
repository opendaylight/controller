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

    public static YangInstanceIdentifier.PathArgument getArgument(String id, DataSchemaNode schemaNode){
        YangInstanceIdentifier.PathArgument value = cache.get(id);
        if(value == null){
            synchronized (cache){
                value = cache.get(id);
                if(value == null) {
                    value = createPathArgument(id, schemaNode);
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
