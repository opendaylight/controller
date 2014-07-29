package org.opendaylight.controller.cluster.datastore.node.utils;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

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
                    value = createPathArgument(id);
                    cache.put(id, value);
                }
            }
        }
        return value;
    }

    private static YangInstanceIdentifier.PathArgument createPathArgument(String id){
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
