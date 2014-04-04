package org.opendaylight.controller.datastore.infinispan;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.infinispan.tree.Fqn;
import org.infinispan.tree.Node;
import org.infinispan.tree.TreeCache;
import org.opendaylight.controller.datastore.infinispan.utils.NamespacePrefixMapper;
import org.opendaylight.controller.datastore.notification.ChangeListenerNotifyTask;
import org.opendaylight.controller.datastore.notification.ListenerRegistrationManager;
import org.opendaylight.controller.datastore.notification.WriteDeleteTransactionTracker;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class ReadWriteTransactionImpl implements DOMStoreReadWriteTransaction, AutoCloseable {

    private final ListeningExecutorService crudExecutor;

    private final SchemaContext schemaContext;
    private final TreeCache treeCache;
    private final String transactionId;
    private Transaction transaction;
    private final WriteDeleteTransactionTracker transactionLog;
    private final ListenerRegistrationManager listenerManager;


    private static final Logger logger = LoggerFactory.getLogger(ReadWriteTransactionImpl.class);

    public ReadWriteTransactionImpl(String storeName, SchemaContext schemaContext, TreeCache treeCache,
                                    ListeningExecutorService crudExecutor,
                                    long transactionNo, ListenerRegistrationManager listenerManager){
        this.schemaContext = schemaContext;
        this.treeCache = treeCache;
        this.crudExecutor = crudExecutor;
        this.transactionId = storeName + "-txn-" + transactionNo;
        this.listenerManager = listenerManager;

        // FIXME : Comment the following statement to enable change logging and notifications
        transactionLog = new WriteDeleteTransactionTracker(transactionNo);
        // FIXME : Uncomment the following statement to enable change logging and notifications
        //transactionLog = null;

        final ListenableFuture future = this.crudExecutor.submit(createInfinispanTransactionWrapper(new BeginTransactionAction(treeCache, schemaContext, transactionLog, crudExecutor)));
        try {
            transaction = (Transaction) future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
  @Override
  public Object getIdentifier() {
    return this.transactionId;
  }

  @Override
  public void close() {

      final TransactionManager transactionManager = treeCache.getCache().getAdvancedCache().getTransactionManager();
      try {
          if(this.transaction != null){
              transactionManager.resume(ReadWriteTransactionImpl.this.transaction);
              transactionManager.rollback();
              this.transaction = null;
          }
      } catch(Exception e){
          logger.info("Exception occurred in transaction : {}", getIdentifier());
          logger.error("Wrapped Call failed", e);
      } finally {
      }
  }

    @Override
    public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(InstanceIdentifier path) {
        if(path != null){
            logger.info("Read : {}", path.toString());
        } else {
            logger.error("Reading from null path");
        }
        final ListenableFuture future = crudExecutor.submit(createInfinispanTransactionWrapper(new ReadAction(schemaContext, treeCache, path)));
        return Futures.transform(future, new Function<NormalizedNode<?,?>, Optional< NormalizedNode <?,?>>>() {
            @Nullable
            @Override
            public Optional<NormalizedNode<?, ?>> apply(@Nullable NormalizedNode<?,?> o) {
                if(o == null){
                    return Optional.absent();
                }
                final Optional<NormalizedNode<?, ?>> of = Optional.<NormalizedNode<?,?>>of(o);
                return of;
            }
        });
    }

    @Override
    public boolean exists(InstanceIdentifier path) {
        if(path != null){
                    logger.info("Read : {}", path.toString());
                } else {
                    logger.error("Reading from null path");
                }
        final ListenableFuture<Boolean> future = crudExecutor.submit(createInfinispanTransactionWrapper(new ExistsAction(schemaContext, treeCache, path)));
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(InstanceIdentifier path, NormalizedNode<?, ?> data) {
      if(path != null){
          if(logger.isTraceEnabled()){
              logger.trace("Write : {}", path.toString());
          }
      } else {
          logger.error("Writing to null path");
      }

      final ListenableFuture future = crudExecutor.submit(createInfinispanTransactionWrapper(new WriteAction(transactionId, schemaContext, treeCache, path, data, transactionLog)));
      try {
          future.get();
      } catch (InterruptedException e) {
          throw new RuntimeException(e);
      } catch (ExecutionException e) {
          throw new RuntimeException(e);
      }
    }



    public Callable createInfinispanTransactionWrapper(Callable wrappedCallable){
        return new InfinispanTransactionWrapper(wrappedCallable);
    }


    @Override
    public void delete(InstanceIdentifier path) {
        final ListenableFuture future = crudExecutor.submit(createInfinispanTransactionWrapper(new DeleteAction(transactionId, schemaContext, treeCache, path, transactionLog)));
        try {
            future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


  @Override
  public DOMStoreThreePhaseCommitCohort ready() {
    if(transactionLog != null) {
        transactionLog.lockTransactionLog();
    }

    return new DOMStoreThreePhaseCommitImpl(transactionLog, listenerManager);
  }

  private static class BeginTransactionAction implements Callable {
    public static int INFINISPAN_TRANSACTION_TIMEOUT = 120; //2 minutes;
    private final TreeCache treeCache;
    private final WriteDeleteTransactionTracker transactionLog;
    private final SchemaContext schemaContext;
    private final ListeningExecutorService executorService;

      public BeginTransactionAction(TreeCache treeCache, SchemaContext sc, final WriteDeleteTransactionTracker wdt, ListeningExecutorService executorService) {
      this.treeCache = treeCache;
      this.transactionLog = wdt;
      this.schemaContext = sc;
      this.executorService = executorService;
    }

    @Override
    public Object call() throws Exception {
      if(transactionLog != null) {

        treeCache.getCache().getAdvancedCache().getTransactionManager().begin();
        Node node = treeCache.getNode(Fqn.fromString("/")) ;
        final NormalizedNode<?, ?> normalizedNode = new NormalizedNodeToTreeCacheCodec(schemaContext, treeCache).decode(InstanceIdentifier.builder().build(), node);
        transactionLog.setSnapshotTree(normalizedNode);


      } else{
          treeCache.getCache().getAdvancedCache().getTransactionManager().begin();
      }
        final Transaction currentTransaction = treeCache.getCache().getAdvancedCache().getTransactionManager().getTransaction();
        return currentTransaction;
    }
  }

  private static class RollbackTransactionAction implements Callable {
    private final TreeCache treeCache;
    private final WriteDeleteTransactionTracker transactionLog;

    public RollbackTransactionAction(TreeCache treeCache, WriteDeleteTransactionTracker transactionLog) {
      this.treeCache = treeCache;
      this.transactionLog = transactionLog;
    }

    @Override
    public Object call() throws Exception {
        try {
            treeCache.getCache().getAdvancedCache().getTransactionManager().rollback();
        } finally {
            if(transactionLog != null) {
              transactionLog.clear();
            }
        }
      return null;
    }
  }

  private static class CommitTransactionAction implements Callable {
    private final TreeCache treeCache;


    public CommitTransactionAction(TreeCache treeCache) {
      this.treeCache = treeCache;
    }

    @Override
    public Object call() throws Exception {
      treeCache.getCache().getAdvancedCache().getTransactionManager().commit();
      return null;
    }
  }

  private static class ReadAction implements Callable<Object> {
    private final TreeCache treeCache;
    private final InstanceIdentifier path;
    private final SchemaContext schemaContext;

    public ReadAction(SchemaContext schemaContext, TreeCache treeCache, InstanceIdentifier path) {
      this.schemaContext = schemaContext;
      this.treeCache = treeCache;
      this.path = path;
    }

      @Override
      public Object call() throws Exception {
          Node node = treeCache.getNode(Fqn.fromString(NamespacePrefixMapper.get().fromInstanceIdentifier(path.toString())));
          if(node == null){
              return null;
          }
          try {
              final NormalizedNode<?, ?> normalizedNode = new NormalizedNodeToTreeCacheCodec(schemaContext, treeCache).decode(path, node);
              if(logger.isTraceEnabled()){
                  logger.trace("Successfully read node : {}" , normalizedNode.toString());
              }
              return normalizedNode;

          } catch(Exception e){
              logger.trace("Failed to read : " + e.getMessage());
              logger.trace("More info", e);
          }
          return null;
      }
  }

  private static class WriteAction implements Callable {

    private final TreeCache treeCache;
    private final InstanceIdentifier path;
    private final NormalizedNode<?, ?> normalizedNode;
    private final SchemaContext schemaContext;
    private final WriteDeleteTransactionTracker transactionLog;
    private final String transactionId;

    public WriteAction(String transactionId, SchemaContext schemaContext, TreeCache treeCache, InstanceIdentifier path, NormalizedNode<?, ?> normalizedNode, final WriteDeleteTransactionTracker transactionLog) {
      this.transactionId = transactionId;
      this.schemaContext = schemaContext;
      this.treeCache = treeCache;
      this.path = path;
      this.normalizedNode = normalizedNode;
      this.transactionLog = transactionLog;
    }

    @Override
    public Object call() throws Exception {
      if(path != null)
        logger.trace("******************* Transaction {} Writing node : {}", transactionId, path.toString());
      new NormalizedNodeToTreeCacheCodec(schemaContext, treeCache).encode(path, normalizedNode, transactionLog);
      return null;
    }
  }

  private static class DeleteAction implements Callable {

    private final TreeCache treeCache;
    private final InstanceIdentifier path;

    private final SchemaContext schemaContext;
    private final WriteDeleteTransactionTracker transactionLog;

    private final String transactionId;

    public DeleteAction(String transactionId, SchemaContext schemaContext, TreeCache treeCache, InstanceIdentifier path, WriteDeleteTransactionTracker transactionLog) {

      this.transactionId = transactionId;
      this.treeCache = treeCache;
      this.path = path;
      this.transactionLog = transactionLog;
      this.schemaContext = schemaContext;
    }

    @Override
    public Object call() throws Exception {
      Fqn removeFqn = Fqn.fromString(NamespacePrefixMapper.get().fromInstanceIdentifier(path.toString()));
      NormalizedNode<?, ?> trackRemovedNode = null;
      if(transactionLog != null){
        trackRemovedNode = new NormalizedNodeToTreeCacheCodec(schemaContext, treeCache).decode(path, treeCache.getNode(removeFqn));
      }
      logger.trace("###################### Transaction {} Removing node : {}", transactionId, removeFqn.toString());
      final boolean removed = treeCache.removeNode(removeFqn);
      logger.trace("###################### Transaction {} Remove node : {}, successful : {}", transactionId, removeFqn.toString(), removed);

      final boolean exists = treeCache.exists(removeFqn);

      logger.trace("###################### Transaction {} Removed node still there? : {}, successful : {}", transactionId, removeFqn.toString(), exists);

      if(transactionLog != null) {
        transactionLog.track(path.toString(), WriteDeleteTransactionTracker.Operation.REMOVED, trackRemovedNode);
      }
      return null;
    }
  }

  private static class ExistsAction implements Callable<Boolean> {
      private final TreeCache treeCache;
      private final InstanceIdentifier path;
      private final SchemaContext schemaContext;

      public ExistsAction(SchemaContext schemaContext, TreeCache treeCache, InstanceIdentifier path) {
        this.schemaContext = schemaContext;
        this.treeCache = treeCache;
        this.path = path;
      }

      @Override
      public Boolean call() throws Exception {
          return treeCache.exists(Fqn.fromString(NamespacePrefixMapper.get().fromInstanceIdentifier(path.toString())));
      }
  }

  private class DOMStoreThreePhaseCommitImpl implements DOMStoreThreePhaseCommitCohort {

    private final WriteDeleteTransactionTracker transactionLog;
    private final ListenerRegistrationManager lrm;


    private List<ChangeListenerNotifyTask> listenerTasks;

    public DOMStoreThreePhaseCommitImpl(final WriteDeleteTransactionTracker transactionLog, ListenerRegistrationManager lrm) {
      this.transactionLog = transactionLog;
      this.lrm = lrm;
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {

      //ok here we need to compute
      return Futures.immediateFuture(true);
    }

    @Override
    public ListenableFuture<Void> preCommit() {
      if(transactionLog != null) {
        listenerTasks = lrm.prepareNotifyTasks(transactionLog);
      }
      return Futures.immediateFuture(null);
    }

      @Override
      public ListenableFuture<Void> abort() {
          final ListenableFuture future = crudExecutor.submit(createInfinispanTransactionWrapper(new RollbackTransactionAction(treeCache, transactionLog)));
          return Futures.transform(future, new Function<Object, Void>() {

              @Nullable
              @Override
              public Void apply(@Nullable Object o) {
                  return null;
              }
          });
      }

      @Override
      public ListenableFuture<Void> commit() {
          logger.trace("COMMITTING TRANSACTION : {}", getIdentifier());
//          final ListenableFuture future = commitExecutor.submit(createInfinispanTransactionWrapper(new CommitTransactionAction(treeCache)));
          final ListenableFuture future = MoreExecutors.sameThreadExecutor().submit(createInfinispanTransactionWrapper(new CommitTransactionAction(treeCache)));
          //TODO : Use a different executor than the crud executor for sending notifications
          future.addListener(new Runnable() {
              @Override
              public void run() {
                  // here we need to call the notify listeners
                  lrm.notifyListeners(listenerTasks);
              } }, lrm.getNotifyExecutor());
          return Futures.transform(future, new Function<Object, Void>() {
              @Nullable
              @Override
              public Void apply(@Nullable Object o) {
                  return null;
              }
          });
      }
    }


    private class InfinispanTransactionWrapper implements Callable {
        private Callable wrappedCallable;

        public InfinispanTransactionWrapper(Callable wrappedCallable){
            this.wrappedCallable = wrappedCallable;
        }

        @Override
        public Object call() throws Exception {
            final TransactionManager transactionManager = treeCache.getCache().getAdvancedCache().getTransactionManager();
            try {
                if(ReadWriteTransactionImpl.this.transaction != null){
                    transactionManager.resume(ReadWriteTransactionImpl.this.transaction);
                }
                return wrappedCallable.call();
            } catch(Exception e){
                logger.info("Exception occurred in transaction : {}", getIdentifier());
                logger.error("Wrapped Call failed", e);
                throw e;
            } finally {
                transactionManager.suspend();
            }
       }
  }

}
