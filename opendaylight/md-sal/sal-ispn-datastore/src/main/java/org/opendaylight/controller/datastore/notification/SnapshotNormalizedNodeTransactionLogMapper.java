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


      if(nodeFqn.toString().startsWith("/(urn:ietf:params:xml:ns:netconf:base:1.0)data")) {
        nodeFqn=nodeFqn.fromString(nodeFqn.toString().substring("/(urn:ietf:params:xml:ns:netconf:base:1.0)data".length()));
      }

      wdtt.track(nodeFqn.toString(), WriteDeleteTransactionTracker.Operation.VISITED, normalizedNode);
    } else {
      if (parentPath == null) {
        parentPath = "";
      }
      Fqn nodeFqn = Fqn.fromRelativeFqn(Fqn.fromString(parentPath), Fqn.fromString(normalizedNode.getIdentifier().toString()));
      if(nodeFqn.toString().startsWith("/(urn:ietf:params:xml:ns:netconf:base:1.0)data")) {
        nodeFqn=nodeFqn.fromString(nodeFqn.toString().substring("/(urn:ietf:params:xml:ns:netconf:base:1.0)data".length()));
      }


      wdtt.track(nodeFqn.toString(), WriteDeleteTransactionTracker.Operation.VISITED, normalizedNode);
    }

  }
}
