package org.opendaylight.controller.datastore.infinispan.utils;

import com.google.common.base.Preconditions;
import org.infinispan.tree.Fqn;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class NormalizedNodeGetter implements NormalizedNodeVisitor{
    private final String path;
    NormalizedNode output;

    public NormalizedNodeGetter(String path){
        Preconditions.checkNotNull(path);
        this.path = path;
    }

    @Override
    public void visitNode(int level, String parentPath, NormalizedNode normalizedNode) {
        Fqn nodeFqn = Fqn.fromRelativeFqn(Fqn.fromString(parentPath), Fqn.fromString(normalizedNode.getIdentifier().toString()));

        if(nodeFqn.toString().equals(path)){
            output = normalizedNode;
        }
    }

    public NormalizedNode getOutput(){
        return output;
    }
}
