package org.opendaylight.controller.cluster.datastore.node.utils;

import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class NormalizedNodeGetter implements
    NormalizedNodeVisitor {
    private final String path;
    NormalizedNode output;

    public NormalizedNodeGetter(String path){
        Preconditions.checkNotNull(path);
        this.path = path;
    }

    @Override
    public void visitNode(int level, String parentPath, NormalizedNode normalizedNode) {
        String nodePath = parentPath + "/"+ normalizedNode.getIdentifier().toString();

        if(nodePath.toString().equals(path)){
            output = normalizedNode;
        }
    }

    public NormalizedNode getOutput(){
        return output;
    }
}
