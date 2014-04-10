package org.opendaylight.controller.datastore.infinispan;

import org.infinispan.tree.Fqn;
import org.infinispan.tree.TreeCache;
import org.opendaylight.controller.datastore.infinispan.utils.InfinispanTreeWrapper;
import org.opendaylight.controller.datastore.infinispan.utils.NormalizedNodeVisitor;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NormalizedNodeTreeCacheWriter implements NormalizedNodeVisitor {
    private final InfinispanTreeWrapper treeCacheWrapper;
    private final TreeCache treeCache;
    private static final Logger logger = LoggerFactory.getLogger(NormalizedNodeTreeCacheWriter.class);

    public NormalizedNodeTreeCacheWriter(TreeCache treeCache){
        this.treeCache = treeCache;
        this.treeCacheWrapper = new InfinispanTreeWrapper();
    }

    @Override
    public void visitNode(int level, String parentPath, NormalizedNode normalizedNode) {

        if(normalizedNode instanceof LeafNode || normalizedNode instanceof LeafSetEntryNode){
            Fqn nodeFqn = Fqn.fromRelativeFqn(Fqn.fromString(parentPath), Fqn.fromString(normalizedNode.getIdentifier().toString()));

            if(logger.isTraceEnabled()){
                traceNode(nodeFqn, normalizedNode);
            }

            treeCacheWrapper.writeValue(treeCache, nodeFqn, normalizedNode.getValue());
        }

    }

    private void traceNode(Fqn nodeFqn, NormalizedNode normalizedNode) {
        logger.trace("\nPutting data");
        logger.trace("----------------------------");
        logger.trace("Fqn : {}", nodeFqn.toString());
        logger.trace("Key : {}", normalizedNode.getKey().toString());
        logger.trace("Value : {}", normalizedNode.getValue().toString());
    }
}
