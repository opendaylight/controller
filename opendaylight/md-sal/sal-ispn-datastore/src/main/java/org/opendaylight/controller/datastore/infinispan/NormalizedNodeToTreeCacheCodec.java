package org.opendaylight.controller.datastore.infinispan;

import org.infinispan.tree.Fqn;
import org.infinispan.tree.Node;
import org.infinispan.tree.TreeCache;
import org.opendaylight.controller.datastore.infinispan.utils.InfinispanTreeWrapper;
import org.opendaylight.controller.datastore.infinispan.utils.NormalizedNodeNavigator;
import org.opendaylight.controller.datastore.infinispan.utils.PathUtils;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.controller.datastore.notification.WriteDeleteTransactionTracker;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class NormalizedNodeToTreeCacheCodec {
    private final SchemaContext ctx;
    private final TreeCache treeCache;
    private static final Logger logger = LoggerFactory.getLogger("TRANSACTION");
    private final InfinispanTreeWrapper treeCacheWrapper = new InfinispanTreeWrapper();

    public NormalizedNodeToTreeCacheCodec(final SchemaContext ctx, final TreeCache treeCache){
        this.ctx = ctx;
        this.treeCache = treeCache;
    }

    public void encode(InstanceIdentifier id, NormalizedNode node,final WriteDeleteTransactionTracker wdtt){
        String parentPath = "";

        if(id != null){
            parentPath = PathUtils.getParentPath(id.toString());
        }

        final NormalizedNodeTreeCacheWriter normalizedNodeTreeCacheWriter = new NormalizedNodeTreeCacheWriter(treeCache, wdtt);
        new NormalizedNodeNavigator(normalizedNodeTreeCacheWriter).navigate(parentPath, node);
        final Map<Fqn,Map<String,Object>> writeableData = normalizedNodeTreeCacheWriter.getWriteableData();

        for(Fqn key : writeableData.keySet()){
            treeCache.put(key, writeableData.get(key));
        }

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
            try {
                return currentOp.normalize(nodeType , node);
            } catch(RuntimeException e){
                throw e;
        }
    }


}
