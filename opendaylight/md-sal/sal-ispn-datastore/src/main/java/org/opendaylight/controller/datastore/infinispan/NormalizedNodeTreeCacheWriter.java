package org.opendaylight.controller.datastore.infinispan;

import org.infinispan.tree.Fqn;
import org.infinispan.tree.TreeCache;
import org.opendaylight.controller.datastore.infinispan.utils.InfinispanTreeWrapper;
import org.opendaylight.controller.datastore.infinispan.utils.NamespacePrefixMapper;
import org.opendaylight.controller.datastore.infinispan.utils.NormalizedNodeVisitor;
import org.opendaylight.controller.datastore.notification.WriteDeleteTransactionTracker;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NormalizedNodeTreeCacheWriter implements NormalizedNodeVisitor {

  private final InfinispanTreeWrapper treeCacheWrapper;
  private final TreeCache treeCache;
  private final WriteDeleteTransactionTracker wdtt;
  private static final Logger logger = LoggerFactory.getLogger(NormalizedNodeTreeCacheWriter.class);

  public NormalizedNodeTreeCacheWriter(TreeCache treeCache, WriteDeleteTransactionTracker wdtt) {
    this.treeCache = treeCache;
    this.treeCacheWrapper = new InfinispanTreeWrapper();
    this.wdtt = wdtt;
  }

  @Override
  public void visitNode(int level, String parentPath, NormalizedNode normalizedNode) {
    String fixedParentPath = NamespacePrefixMapper.get().fromInstanceIdentifier(parentPath);
    String fixedIdentifier = NamespacePrefixMapper.get().fromInstanceIdentifier(normalizedNode.getIdentifier().toString());

    if (normalizedNode instanceof LeafNode || normalizedNode instanceof LeafSetEntryNode) {
      Fqn nodeFqn = Fqn.fromRelativeFqn(Fqn.fromString(fixedParentPath), Fqn.fromString(fixedIdentifier));

        if(logger.isTraceEnabled()){
            traceNode(nodeFqn, normalizedNode);
        }

      treeCacheWrapper.writeValue(treeCache, nodeFqn, normalizedNode.getValue());

      wdtt.track(nodeFqn.toString(), WriteDeleteTransactionTracker.Operation.UPDATED, normalizedNode);
    } else {
      if (parentPath == null) {
        parentPath = "";
      }
      Fqn nodeFqn = Fqn.fromRelativeFqn(Fqn.fromString(parentPath), Fqn.fromString(normalizedNode.getIdentifier().toString()));
      wdtt.track(nodeFqn.toString(), WriteDeleteTransactionTracker.Operation.VISITED, normalizedNode);
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
