package org.opendaylight.controller.datastore.infinispan;

import org.infinispan.tree.Fqn;
import org.infinispan.tree.TreeCache;
import org.opendaylight.controller.datastore.infinispan.utils.InfinispanTreeWrapper;
import org.opendaylight.controller.datastore.infinispan.utils.NormalizedNodeVisitor;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class NormalizedNodeTreeCacheWriter implements NormalizedNodeVisitor {
    private final InfinispanTreeWrapper treeCacheWrapper;
    private final TreeCache treeCache;

    public NormalizedNodeTreeCacheWriter(TreeCache treeCache){
        this.treeCache = treeCache;
        this.treeCacheWrapper = new InfinispanTreeWrapper();
    }

    @Override
    public void visitNode(int level, String parentPath, NormalizedNode normalizedNode) {

        if(normalizedNode instanceof LeafNode || normalizedNode instanceof LeafSetEntryNode){
            Fqn nodeFqn = Fqn.fromRelativeFqn(Fqn.fromString(parentPath), Fqn.fromString(normalizedNode.getIdentifier().toString()));

            System.out.println("\nPutting data");
            System.out.println("----------------------------");
            System.out.println("Fqn : " + nodeFqn.toString());
            System.out.println("Key : " + normalizedNode.getKey());
            System.out.println("Value : " + normalizedNode.getValue());

            treeCacheWrapper.writeValue(treeCache, nodeFqn, normalizedNode.getValue());
        }

    }
}
