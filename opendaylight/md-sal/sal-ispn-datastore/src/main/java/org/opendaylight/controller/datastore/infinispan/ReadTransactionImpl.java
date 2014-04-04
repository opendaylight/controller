package org.opendaylight.controller.datastore.infinispan;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.infinispan.tree.Fqn;
import org.infinispan.tree.Node;
import org.infinispan.tree.TreeCache;
import org.opendaylight.controller.datastore.infinispan.utils.NamespacePrefixMapper;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadTransactionImpl implements DOMStoreReadTransaction {


    private final TreeCache treeCache;
    private final SchemaContext schemaContext;
    private static final Logger logger = LoggerFactory.getLogger(ReadTransactionImpl.class);

    public ReadTransactionImpl(TreeCache treeCache, SchemaContext schemaContext) {
        this.treeCache = treeCache;
        this.schemaContext = schemaContext;
    }

    @Override
    public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(InstanceIdentifier path) {
        Node node = treeCache.getNode(Fqn.fromString(NamespacePrefixMapper.get().fromInstanceIdentifier(path.toString())));
        if(node == null){
            return Futures.immediateFuture(Optional.<NormalizedNode<?, ?>>absent());
        }
        try {
            final NormalizedNode<?, ?> normalizedNode = new NormalizedNodeToTreeCacheCodec(schemaContext, treeCache).decode(path, node);
            if(logger.isTraceEnabled()){
                logger.trace("Successfully read node : {}" , normalizedNode.toString());
            }
            return Futures.immediateFuture(Optional.<NormalizedNode<?,?>>of(normalizedNode));

        } catch(Exception e){
            logger.trace("Failed to read : " + e.getMessage());
            logger.trace("More info", e);
        }
        return Futures.immediateFuture(Optional.<NormalizedNode<?, ?>>absent());
    }

    @Override
    public boolean exists(InstanceIdentifier path) {
        return treeCache.exists(Fqn.fromString(NamespacePrefixMapper.get().fromInstanceIdentifier(path.toString())));
    }

    @Override
    public Object getIdentifier() {
        return "read-txn";
    }

    @Override
    public void close() {
    }
}
