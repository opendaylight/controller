package org.opendaylight.controller.datastore.infinispan;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.infinispan.tree.TreeCache;
import org.opendaylight.controller.datastore.ispn.TreeCacheManager;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class DataStoreImpl implements DOMStore, Identifiable<String>, SchemaContextListener {
    public static String DEFAULT_STORE_CACHE_NAME  = "ISPN_DATA_STORE_CACHE";
    private final TreeCache store;
    private final AtomicLong counter = new AtomicLong();
    private SchemaContext schemaContext;

    public DataStoreImpl(TreeCacheManager treeCacheManager){
        this.store = treeCacheManager.getCache(DEFAULT_STORE_CACHE_NAME);
    }

    private ListeningExecutorService createExecutor(){
        return MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    }

    private DOMStoreReadWriteTransaction createTransaction(){
        return new ReadWriteTransactionActor(schemaContext, store, createExecutor(), counter.incrementAndGet());
    }


    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return createTransaction();
    }

    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return createTransaction();
    }

    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return createTransaction();
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


}
