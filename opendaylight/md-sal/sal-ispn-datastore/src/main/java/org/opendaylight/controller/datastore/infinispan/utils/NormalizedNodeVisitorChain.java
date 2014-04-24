package org.opendaylight.controller.datastore.infinispan.utils;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.util.ArrayList;
import java.util.List;

public class NormalizedNodeVisitorChain implements NormalizedNodeVisitor {
    private final List<NormalizedNodeVisitor> list = new ArrayList<>();

    @Override
    public void visitNode(int level, String parentPath, NormalizedNode normalizedNode) {
        for(NormalizedNodeVisitor visitor : list){
            visitor.visitNode(level, parentPath, normalizedNode);
        }
    }

    public void add(NormalizedNodeVisitor normalizedNodeVisitor){
        list.add(normalizedNodeVisitor);
    }
}
