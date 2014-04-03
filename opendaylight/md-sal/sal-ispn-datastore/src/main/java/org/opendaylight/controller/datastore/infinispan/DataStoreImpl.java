package org.opendaylight.controller.datastore.infinispan;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.infinispan.tree.Fqn;
import org.infinispan.tree.Node;
import org.infinispan.tree.TreeCache;
import org.opendaylight.controller.datastore.ispn.TreeCacheManager;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

import java.util.concurrent.atomic.AtomicLong;

public class DataStoreImpl implements DOMStore, Identifiable<String>, SchemaContextListener {
    public static String DEFAULT_STORE_CACHE_NAME  = "ISPN_DATA_STORE_CACHE";
    private final TreeCache store;
    private final AtomicLong counter = new AtomicLong();
    private SchemaContext schemaContext;

    public DataStoreImpl(SchemaContext schemaContext){
        Preconditions.checkNotNull(schemaContext, "schemaContext should not be null");
        this.schemaContext = schemaContext;
        this.store = new TreeCacheManager().getCache(DEFAULT_STORE_CACHE_NAME);
    }

    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new DOMStoreReadTransactionImpl(counter.incrementAndGet(), schemaContext, store);
    }

    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new DOMStoreWriteTransactionImpl(counter.incrementAndGet(), schemaContext, store);
    }

    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return new DOMStoreReadWriteTransactionImpl(counter.incrementAndGet(), schemaContext, store);
    }

    public <L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> ListenerRegistration<L> registerChangeListener(InstanceIdentifier path, L listener, AsyncDataBroker.DataChangeScope scope) {
        return null;
    }

    public String getIdentifier() {
        return "ispn-datastore";
    }

    public void onGlobalContextUpdated(SchemaContext context) {
        this.schemaContext = context;
    }

    private static class DOMStoreReadTransactionImpl extends AbstractDOMStoreTransaction implements DOMStoreReadTransaction{

        protected DOMStoreReadTransactionImpl(long transactionNo, SchemaContext schemaContext, TreeCache treeCache) {
            super(transactionNo, schemaContext, treeCache);
        }

        public ListenableFuture<Optional<NormalizedNode<?,?>>> read(InstanceIdentifier path) {
            Node node = getTreeCache().getNode(Fqn.fromString(path.toString()));
            final NormalizedNode<?, ?> normalizedNode = new NormalizedNodeToTreeCacheCodec(getSchemaContext(), getTreeCache()).decode(path, node);
            final Optional<NormalizedNode<?, ?>> optional = Optional.<NormalizedNode<?,?>>of(normalizedNode);
            return Futures.immediateFuture(optional);
        }
    }

    private static class DOMStoreWriteTransactionImpl extends AbstractDOMStoreTransaction implements DOMStoreWriteTransaction {
        protected DOMStoreWriteTransactionImpl(long transactionNo, SchemaContext schemaContext, TreeCache treeCache) {
            super(transactionNo, schemaContext, treeCache);
        }

        public void write(InstanceIdentifier path, NormalizedNode<?, ?> data) {
            new NormalizedNodeToTreeCacheCodec(getSchemaContext(), getTreeCache()).encode(path, data);
        }

        public void delete(InstanceIdentifier path) {
            getTreeCache().removeNode(Fqn.fromString(path.toString()));
        }

        public DOMStoreThreePhaseCommitCohort ready() {
            return new DOMStoreThreePhaseCommitCohortImpl(this);
        }
    }

    private static class DOMStoreReadWriteTransactionImpl extends AbstractDOMStoreTransaction implements DOMStoreReadWriteTransaction {

        protected DOMStoreReadWriteTransactionImpl(long transactionNo, SchemaContext schemaContext, TreeCache treeCache) {
            super(transactionNo, schemaContext, treeCache);
        }

        public ListenableFuture<Optional<NormalizedNode<?,?>>> read(InstanceIdentifier path) {
            Node node = getTreeCache().getNode(Fqn.fromString(path.toString()));
            final NormalizedNode<?, ?> normalizedNode = new NormalizedNodeToTreeCacheCodec(getSchemaContext(), getTreeCache()).decode(path, node);
            final Optional<NormalizedNode<?, ?>> optional = Optional.<NormalizedNode<?,?>>of(normalizedNode);
            return Futures.immediateFuture(optional);
        }

        public void write(InstanceIdentifier path, NormalizedNode<?, ?> data) {
            new NormalizedNodeToTreeCacheCodec(getSchemaContext(), getTreeCache()).encode(path, data);
        }

        public void delete(InstanceIdentifier path) {
            getTreeCache().removeNode(Fqn.fromString(path.toString()));
        }

        public DOMStoreThreePhaseCommitCohort ready() {
            return new DOMStoreThreePhaseCommitCohortImpl(this);
        }
    }
}
