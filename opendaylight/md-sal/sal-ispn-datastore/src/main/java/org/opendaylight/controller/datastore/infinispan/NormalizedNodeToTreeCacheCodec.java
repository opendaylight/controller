package org.opendaylight.controller.datastore.infinispan;

import org.infinispan.tree.Node;
import org.infinispan.tree.TreeCache;
import org.opendaylight.controller.datastore.infinispan.utils.NormalizedNodeNavigator;
import org.opendaylight.controller.datastore.notification.WriteDeleteTransactionTracker;
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

    public void encode(InstanceIdentifier id, NormalizedNode node,final WriteDeleteTransactionTracker wdtt){
        String parentPath = "";

        if(id != null){
            parentPath = id.toString();
            String[] parentPaths = parentPath.split("/");
            if(parentPaths[parentPaths.length-1].equals(node.getIdentifier().toString())){
                if(parentPaths.length > 2){
                    for(int i=0;i<parentPaths.length-2;i++){
                        parentPath += "/" + parentPaths + 1;
                    }
                } else {
                    parentPath = "/";
                }
            }
        }

        new NormalizedNodeNavigator(new NormalizedNodeTreeCacheWriter(treeCache,wdtt)).navigate(parentPath, node);

    }

    public NormalizedNode<?,?> decode(InstanceIdentifier id, Node node){
        DataNormalizationOperation currentOp = DataNormalizationOperation.from(ctx);

        for(InstanceIdentifier.PathArgument pathArgument : id.getPath()){
            currentOp = currentOp.getChild(pathArgument);
        }
        if(!id.getPath().isEmpty())
          return currentOp.normalize(id.getPath().get(id.getPath().size() - 1).getNodeType(), node);
        else
          return currentOp.normalize(null,node);
    }


}
