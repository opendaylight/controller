package org.opendaylight.controller.datastore.notification;

import org.infinispan.tree.Fqn;
import org.opendaylight.controller.datastore.infinispan.utils.NormalizedNodeVisitor;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class SnapshotNormalizedNodeTransactionLogMapper implements NormalizedNodeVisitor {
  private final WriteDeleteTransactionTracker wdtt;

  public SnapshotNormalizedNodeTransactionLogMapper(WriteDeleteTransactionTracker wdtt) {
    this.wdtt = wdtt;
  }

  @Override
  public void visitNode(int level, String parentPath, NormalizedNode normalizedNode) {

    if (normalizedNode instanceof LeafNode || normalizedNode instanceof LeafSetEntryNode) {
      Fqn nodeFqn = Fqn.fromRelativeFqn(Fqn.fromString(parentPath), Fqn.fromString(normalizedNode.getIdentifier().toString()));

      System.out.println("\nPutting data");
      System.out.println("----------------------------");
      System.out.println("Fqn : " + nodeFqn.toString());
      System.out.println("Key : " + normalizedNode.getKey());
      System.out.println("Value : " + normalizedNode.getValue());

      wdtt.track(nodeFqn.toString(), WriteDeleteTransactionTracker.Operation.VISITED, normalizedNode);
    } else {
      if (parentPath == null) {
        parentPath = "";
      }
      Fqn nodeFqn = Fqn.fromRelativeFqn(Fqn.fromString(parentPath), Fqn.fromString(normalizedNode.getIdentifier().toString()));
      wdtt.track(nodeFqn.toString(), WriteDeleteTransactionTracker.Operation.VISITED, normalizedNode);
    }

  }
}
