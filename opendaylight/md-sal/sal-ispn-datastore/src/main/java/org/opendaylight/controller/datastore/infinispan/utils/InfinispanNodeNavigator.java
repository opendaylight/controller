package org.opendaylight.controller.datastore.infinispan.utils;

import org.infinispan.tree.Node;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import java.util.Set;
import java.util.regex.Pattern;

public class InfinispanNodeNavigator {
    int level = 0;
    public InfinispanNodeNavigator(){

    }

    public void navigate(Node node){
        level++;
        String[] paths = node.getFqn().toString().split("/");
        if(paths.length > 1){
            final InstanceIdentifier.PathArgument argument = NodeIdentifierFactory.getArgument(paths[paths.length - 1]);
//            System.out.println(spaces(level*4) + argument.getNodeType().getLocalName());
            String namespace = "(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test?revision=2014-04-15)";
            String fqId = node.getFqn().toString();
            System.out.println(fqId.replaceAll(Pattern.quote(namespace), ""));
        }

        final Set<Node> children = node.getChildren();

        for(Node child : children){
            navigate(child);
        }

        level--;
    }

    private String spaces(int n){
        StringBuilder builder = new StringBuilder();
        for(int i=0;i<n;i++){
            builder.append(' ');
        }
        return builder.toString();
    }

}
