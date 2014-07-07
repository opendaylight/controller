package org.opendaylight.controller.cluster.datastore.node.utils;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import java.util.HashMap;
import java.util.Map;

public class NodeIdentifierFactory {
    private static final Map<String, InstanceIdentifier.PathArgument> cache = new HashMap<>();
    public static InstanceIdentifier.PathArgument getArgument(String id){
        InstanceIdentifier.PathArgument value = cache.get(id);
        if(value == null){
            synchronized (cache){
                value = cache.get(id);
                if(value == null) {
                    value = createPathArgument(id);
                    cache.put(id, value);
                }
            }
        }
        return value;
    }

    private static InstanceIdentifier.PathArgument createPathArgument(String id){
        final NodeIdentifierWithPredicatesGenerator
            nodeIdentifierWithPredicatesGenerator = new NodeIdentifierWithPredicatesGenerator(id);
        if(nodeIdentifierWithPredicatesGenerator.matches()){
            return nodeIdentifierWithPredicatesGenerator.getPathArgument();
        }

        final NodeIdentifierWithValueGenerator
            nodeWithValueGenerator = new NodeIdentifierWithValueGenerator(id);
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
