package org.opendaylight.controller.datastore.infinispan;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.infinispan.tree.Fqn;
import org.infinispan.tree.Node;
import org.infinispan.tree.TreeCache;
import org.opendaylight.controller.datastore.ispn.TreeCacheManager;
import org.opendaylight.controller.datastore.notification.ChangeListenerNotifyTask;
import org.opendaylight.controller.datastore.notification.ListenerRegistrationManager;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class DataStoreImpl implements DOMStore, Identifiable<String>, SchemaContextListener {
    public static final String DEFAULT_STORE_CACHE_NAME  = "ISPN_DATA_STORE_CACHE";
    private static final Logger LOG = LoggerFactory.getLogger(DataStoreImpl.class);
    private final TreeCache store;
    private final AtomicLong counter = new AtomicLong();
    private SchemaContext schemaContext;
    private final String name;
    private final ListeningExecutorService notifyExecutor;

  //this will manage the notification events
  private final ListenerRegistrationManager listenerManager;


  public DataStoreImpl(String storeName, SchemaContext schemaContext,final ListeningExecutorService asyncExecutor){
    this.notifyExecutor =Preconditions.checkNotNull(asyncExecutor);
    name = Preconditions.checkNotNull(storeName);
    Preconditions.checkNotNull(schemaContext, "schemaContext should not be null");
    this.schemaContext = schemaContext;
    this.store = new TreeCacheManager().getCache(storeName);
    listenerManager = new ListenerRegistrationManager();
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

  @Override
  public <L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> ListenerRegistration<L>
  registerChangeListener(final InstanceIdentifier path, final L listener, final AsyncDataBroker.DataChangeScope scope) {
    LOG.debug("{}: Registering data change listener {} for {}",name,listener,path);
    final DataChangeListenerRegistration<L> reg;


    synchronized (this) {
      //we want to registern listener and check that if the path is in cache.
      reg = listenerManager.register(path, listener, scope);
      Node node = store.getNode(Fqn.fromString(path.toString()));
      final NormalizedNode<?, ?> normalizedNode = new NormalizedNodeToTreeCacheCodec(this.schemaContext, this.store).decode(path, node);
      final Optional<NormalizedNode<?, ?>> currentState = Optional.<NormalizedNode<?,?>>of(normalizedNode);

      if (currentState.isPresent()) {
        final NormalizedNode<?, ?> data = currentState.get();

        final DOMImmutableDataChangeEvent event = DOMImmutableDataChangeEvent.builder() //
            .setAfter(data) //
            .addCreated(path, data) //
            .build();
        this.notifyExecutor.submit(new ChangeListenerNotifyTask(Collections.singletonList(reg), event));
      }
    }

    return reg;
  }

    public String getIdentifier() {
        return "ispn-datastore";
    }

    public void onGlobalContextUpdated(SchemaContext context) {
        this.schemaContext = context;
    }


}
