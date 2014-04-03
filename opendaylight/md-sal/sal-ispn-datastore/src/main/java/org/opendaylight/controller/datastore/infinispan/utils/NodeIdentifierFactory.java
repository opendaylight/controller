package org.opendaylight.controller.datastore.infinispan.utils;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public class NodeIdentifierFactory {
    public static InstanceIdentifier.PathArgument getArgument(String id){
        final NodeIdentifierWithPredicatesGenerator nodeIdentifierWithPredicatesGenerator = new NodeIdentifierWithPredicatesGenerator(id);
        if(nodeIdentifierWithPredicatesGenerator.matches()){
            return nodeIdentifierWithPredicatesGenerator.getPathArgument();
        }

        final NodeIdentifierWithValueGenerator nodeWithValueGenerator = new NodeIdentifierWithValueGenerator(id);
        if(nodeWithValueGenerator.matches()){
            return nodeWithValueGenerator.getPathArgument();
        }

        return new NodeIdentifierGenerator(id).getArgument();
    }
}
