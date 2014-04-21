package org.opendaylight.controller.datastore.infinispan;

import org.infinispan.tree.Node;
import org.infinispan.tree.TreeCache;
import org.opendaylight.controller.datastore.infinispan.utils.NormalizedNodeNavigator;
import org.opendaylight.controller.datastore.infinispan.utils.PathUtils;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class NormalizedNodeToTreeCacheCodec {
    private final SchemaContext ctx;
    private final TreeCache treeCache;

    public NormalizedNodeToTreeCacheCodec(final SchemaContext ctx, final TreeCache treeCache){
        this.ctx = ctx;
        this.treeCache = treeCache;
    }

    public void encode(InstanceIdentifier id, NormalizedNode node){
        String parentPath = "";

        if(id != null){
            parentPath = PathUtils.getParentPath(id.toString());
        }

        new NormalizedNodeNavigator(new NormalizedNodeTreeCacheWriter(treeCache)).navigate(parentPath, node);
    }

    public NormalizedNode<?,?> decode(InstanceIdentifier id, Node node){
        DataNormalizationOperation currentOp = DataNormalizationOperation.from(ctx);

        for(InstanceIdentifier.PathArgument pathArgument : id.getPath()){
            currentOp = currentOp.getChild(pathArgument);
        }

        QName nodeType = null;

        if(id.getPath().size() < 1){
            nodeType = null;
        } else {
            final InstanceIdentifier.PathArgument pathArgument = id.getPath().get(id.getPath().size() - 1);
            if(pathArgument instanceof InstanceIdentifier.AugmentationIdentifier){
                nodeType = null;
            } else {
                nodeType = pathArgument.getNodeType();
            }
        }
        return currentOp.normalize(nodeType , node);
    }

}
