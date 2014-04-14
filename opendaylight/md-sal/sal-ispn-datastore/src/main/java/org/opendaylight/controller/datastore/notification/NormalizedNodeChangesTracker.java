package org.opendaylight.controller.datastore.notification;

import org.infinispan.tree.Node;
import org.infinispan.tree.TreeCache;
import org.opendaylight.controller.datastore.infinispan.DataNormalizationOperation;
import org.opendaylight.controller.datastore.infinispan.NormalizedNodeTreeCacheWriter;
import org.opendaylight.controller.datastore.infinispan.utils.NormalizedNodeNavigator;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class NormalizedNodeChangesTracker {
    private final SchemaContext ctx;
    private final TreeCache treeCache;

    public NormalizedNodeChangesTracker(final SchemaContext ctx, final TreeCache treeCache){
        this.ctx = ctx;
        this.treeCache = treeCache;
    }

    public void encode(InstanceIdentifier id, NormalizedNode node){
        String parentPath = "";

        if(id != null){
            parentPath = id.toString();
        }

        new NormalizedNodeNavigator(new NormalizedNodeTreeCacheWriter(treeCache)).navigate(parentPath, node);
    }

    public NormalizedNode<?,?> decode(InstanceIdentifier id, Node node){
        DataNormalizationOperation currentOp = DataNormalizationOperation.from(ctx);

        for(InstanceIdentifier.PathArgument pathArgument : id.getPath()){
            currentOp = currentOp.getChild(pathArgument);
        }

        return currentOp.normalize(id.getPath().get(id.getPath().size() - 1).getNodeType(), node);
    }

}
