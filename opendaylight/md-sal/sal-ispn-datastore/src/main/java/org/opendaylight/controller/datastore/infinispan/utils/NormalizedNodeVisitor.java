package org.opendaylight.controller.datastore.infinispan.utils;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public interface NormalizedNodeVisitor {
    public void visitNode(int level, String parentPath, NormalizedNode normalizedNode);
}
