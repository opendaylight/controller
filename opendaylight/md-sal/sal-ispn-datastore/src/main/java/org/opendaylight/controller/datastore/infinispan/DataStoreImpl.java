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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class DataStoreImpl implements DOMStore, Identifiable<String>, SchemaContextListener {
  public static final String DEFAULT_STORE_CACHE_NAME = "ISPN_DATA_STORE_CACHE";
  private static final Logger LOG = LoggerFactory.getLogger(DataStoreImpl.class);

  private final TreeCache store;
  private final AtomicLong counter = new AtomicLong();
  private SchemaContext schemaContext;
  private final String name;
  private final ListeningExecutorService notifyExecutor;

  //this will manage the notification events listeners
  private final ListenerRegistrationManager listenerManager;
  static private TreeCacheManager treeCacheManager;


  public DataStoreImpl(String storeName, SchemaContext schemaContext, final ListeningExecutorService asyncExecutor) {
    this.notifyExecutor = Preconditions.checkNotNull(asyncExecutor);
    name = Preconditions.checkNotNull(storeName);
    Preconditions.checkNotNull(schemaContext, "schemaContext should not be null");
    this.schemaContext = schemaContext;
    this.store = getTreeCacheManager().getCache(storeName);
    listenerManager = new ListenerRegistrationManager(this.notifyExecutor);
  }

  private ListeningExecutorService createExecutor() {
    return MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
  }

  private DOMStoreReadWriteTransaction createTransaction() {
    ReadWriteTransactionActor rwta = new ReadWriteTransactionActor(schemaContext, store, createExecutor(), counter.incrementAndGet());
    rwta.setListenerManager(listenerManager);
    return rwta;
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
    LOG.debug("{}: Registering data change listener {} for {}", name, listener, path);

    return this.listenerManager.register(path, listener, scope, store, schemaContext);
  }

  //for testing purpose only -- need to removed later
  public Map<String, Collection<?>> listeners() {
    Map<String, RegisterListenerNode> pathListeners = this.listenerManager.listeners();
    Map<String, Collection<?>> listenersMapping = new HashMap<String, Collection<?>>();
    for (Map.Entry<String, RegisterListenerNode> registration : pathListeners.entrySet()) {
      Collection<DataChangeListenerRegistration<?>> listeners = registration.getValue().getListeners();
      Collection<AsyncDataChangeListener> actualOnes = new ArrayList<AsyncDataChangeListener>();
      for (DataChangeListenerRegistration dclr : listeners) {
        actualOnes.add(dclr.getInstance());
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


  public static TreeCacheManager getTreeCacheManager() {

    if (treeCacheManager == null) {
      treeCacheManager = new TreeCacheManager();
    }
    return treeCacheManager;
  }

  public static void setTreeCacheManager(TreeCacheManager tcm) {
    treeCacheManager = tcm;
  }
}
