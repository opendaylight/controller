package org.opendaylight.controller.datastore.infinispan;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.infinispan.tree.TreeCache;
import org.opendaylight.controller.datastore.ispn.TreeCacheManager;
import org.opendaylight.controller.datastore.notification.ListenerRegistrationManager;
import org.opendaylight.controller.datastore.notification.RegisterListenerNode;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class DataStoreImpl implements DOMStore, Identifiable<String>, SchemaContextListener {
    public static String DEFAULT_STORE_CACHE_NAME  = "ISPN_DATA_STORE_CACHE";
    private final TreeCache store;
    private final AtomicLong counter = new AtomicLong();
    private SchemaContext schemaContext;
    private static final Logger logger = LoggerFactory.getLogger(DataStoreImpl.class);
    private final ListeningExecutorService notifyExecutor;
    private final String name;

    //this will manage the notification events listeners
    private final ListenerRegistrationManager listenerManager;

    private static final Logger LOG = LoggerFactory.getLogger(DataStoreImpl.class);

    public DataStoreImpl(TreeCache store, String storeName, SchemaContext schemaContext, final ListeningExecutorService asyncExecutor) {
        this.notifyExecutor = Preconditions.checkNotNull(asyncExecutor);
        name = Preconditions.checkNotNull(storeName);
        this.schemaContext = schemaContext;
        this.store = store;
        listenerManager = new ListenerRegistrationManager(this.notifyExecutor);
    }

    public DataStoreImpl(String storeName, SchemaContext schemaContext, final ListeningExecutorService asyncExecutor) {
        this(TreeCacheManager.get().getCache(DEFAULT_STORE_CACHE_NAME + "-" + storeName), storeName, schemaContext, asyncExecutor);
    }


    public DOMStoreReadTransaction newReadOnlyTransaction() {
        final ReadTransactionImpl transaction = new ReadTransactionImpl(store, schemaContext);
        logger.trace("READ-ONLY TRANSACTION : {}", transaction.getIdentifier());
        return transaction;
    }

    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        final DOMStoreReadWriteTransaction transaction = createTransaction();
        logger.trace("WRITE-ONLY TRANSACTION : {}", transaction.getIdentifier());
        return transaction;

    }

    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        final DOMStoreReadWriteTransaction transaction = createTransaction();
        logger.trace("READ-WRITE TRANSACTION : {}", transaction.getIdentifier());
        return transaction;

    }
  private ListeningExecutorService createExecutor() {
    return MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
  }

  private DOMStoreReadWriteTransaction createTransaction() {
      final ListeningExecutorService executor = MoreExecutors.sameThreadExecutor();
      ReadWriteTransactionImpl rwta = new ReadWriteTransactionImpl(name, schemaContext, store, executor, counter.incrementAndGet(),listenerManager);
    return rwta;
  }


  @Override
  public <L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> ListenerRegistration<L>
  registerChangeListener(final InstanceIdentifier path, final L listener, final AsyncDataBroker.DataChangeScope scope) {
    LOG.debug("{}: Registering data change listener {} for {}", name, listener, path);

    return this.listenerManager.register(path, listener, scope, store, schemaContext);
  }

  //for testing purpose only -- need to removed later
  public Map<String, Collection<?>> listeners() {
    Map<String, RegisterListenerNode> pathListeners = this.listenerManager.listeners();
    Map<String, Collection<?>> listenersMapping = new HashMap<String, Collection<?>>();
    for (Map.Entry<String, RegisterListenerNode> registration : pathListeners.entrySet()) {
      Collection<RegisterListenerNode.DataChangeListenerRegistration<?>> listeners = registration.getValue().getListeners();
      Collection<AsyncDataChangeListener> actualOnes = new ArrayList<AsyncDataChangeListener>();
      for (RegisterListenerNode.DataChangeListenerRegistration dclr : listeners) {
        actualOnes.add((AsyncDataChangeListener)dclr.getInstance());
      }
      listenersMapping.put(registration.getKey(), actualOnes);
    }
    return listenersMapping;
  }

  public String getIdentifier() {
    return name;
  }

  public void onGlobalContextUpdated(SchemaContext context) {
    this.schemaContext = context;
  }

}
