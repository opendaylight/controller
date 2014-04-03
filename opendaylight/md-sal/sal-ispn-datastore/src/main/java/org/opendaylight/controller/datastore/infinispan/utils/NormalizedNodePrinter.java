package org.opendaylight.controller.datastore.infinispan.utils;

import org.opendaylight.controller.datastore.infinispan.utils.NormalizedNodeVisitor;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class NormalizedNodePrinter implements NormalizedNodeVisitor {

    private String spaces(int n){
        StringBuilder builder = new StringBuilder();
        for(int i=0;i<n;i++){
            builder.append(' ');
        }
        return builder.toString();
    }

    @Override
    public void visitNode(int level, String parentPath, NormalizedNode normalizedNode) {
        System.out.println(spaces((level) * 4) + normalizedNode.getClass().toString() + ":" + normalizedNode.getIdentifier());
        if(normalizedNode instanceof LeafNode || normalizedNode instanceof LeafSetEntryNode){
            System.out.println(spaces((level) * 4) + " parentPath = " + parentPath);
            System.out.println(spaces((level) * 4) + " key = " + normalizedNode.getClass().toString() + ":" + normalizedNode.getKey());
            System.out.println(spaces((level) * 4) + " value = " + normalizedNode.getValue());
        }
    }
}
