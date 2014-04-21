package org.opendaylight.controller.datastore.infinispan;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.infinispan.tree.Fqn;
import org.infinispan.tree.Node;
import org.infinispan.tree.TreeCache;
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

public class ReadWriteTransactionActor implements DOMStoreReadWriteTransaction, AutoCloseable {

    private final ListeningExecutorService crudExecutor;
    private final ListeningExecutorService commitExecutor;

    private final SchemaContext schemaContext;
    private final TreeCache treeCache;
    private final String transactionId;
    private final Transaction transaction;
    private final WriteDeleteTransactionTracker transactionLog;
    private final ListenerRegistrationManager listenerManager;


    private static final Logger logger = LoggerFactory.getLogger(ReadWriteTransactionActor.class);

    public ReadWriteTransactionActor(SchemaContext schemaContext, TreeCache treeCache,
                                     ListeningExecutorService crudExecutor, ListeningExecutorService commitExecutor,
                                     long transactionNo, ListenerRegistrationManager listenerManager, boolean readOnly){
        this.schemaContext = schemaContext;
        this.treeCache = treeCache;
        this.crudExecutor = crudExecutor;
        this.commitExecutor = commitExecutor;
        this.transactionId = "ispn-txn-" + transactionNo;
        this.listenerManager = listenerManager;

        if(!readOnly) {  //only for write transaction
          transactionLog = new WriteDeleteTransactionTracker(transactionNo);
        }else{           //read-only will not have LRM.
          transactionLog = null;

        }

        final ListenableFuture future = this.crudExecutor.submit(createInfinispanTransactionWrapper(new BeginTransactionAction(treeCache, schemaContext, transactionLog)));
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
//    final ListenableFuture future = crudExecutor.submit(new RollbackTransactionAction(treeCache, transactionLog));
//    try {
//      future.get();
//    } catch (InterruptedException e) {
//      throw new RuntimeException(e);
//    } catch (ExecutionException e) {
//      throw new RuntimeException(e);
//    }
//
//    crudExecutor.shutdown();
//    commitExecutor.shutdown();
  }

    @Override
    public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(InstanceIdentifier path) {
        if(path != null){
            if(logger.isTraceEnabled()){
                logger.trace("Read : {}", path.toString());
            }
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
    public void write(InstanceIdentifier path, NormalizedNode<?, ?> data) {
      if(path != null){
          if(logger.isTraceEnabled()){
              logger.trace("Write : {}", path.toString());
          }
      } else {
          logger.error("Writing to null path");
      }

      final ListenableFuture future = crudExecutor.submit(createInfinispanTransactionWrapper(new WriteAction(schemaContext, treeCache, path, data, transactionLog)));
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
        final ListenableFuture future = crudExecutor.submit(createInfinispanTransactionWrapper(new DeleteAction(schemaContext, treeCache, path, transactionLog)));
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
    transactionLog.lockTransactionLog();

    return new DOMStoreThreePhaseCommitImpl(transactionLog, listenerManager);
  }

  private static class BeginTransactionAction implements Callable {
    public static int INFINISPAN_TRANSACTION_TIMEOUT = 120; //2 minutes;
    private final TreeCache treeCache;
    private final WriteDeleteTransactionTracker wdtt;
    private final SchemaContext schemaContext;

    public BeginTransactionAction(TreeCache treeCache, SchemaContext sc, final WriteDeleteTransactionTracker wdt) {
      this.treeCache = treeCache;
      this.wdtt = wdt;
      this.schemaContext = sc;
    }

    @Override
    public Object call() throws Exception {
      if(wdtt != null) {
        treeCache.getCache().getAdvancedCache().getTransactionManager().setTransactionTimeout(INFINISPAN_TRANSACTION_TIMEOUT);    //TODO: REMOVE THIS
        treeCache.getCache().getAdvancedCache().getTransactionManager().begin();
        Node node = treeCache.getNode(Fqn.fromString("/")) ;
        final NormalizedNode<?, ?> normalizedNode = new NormalizedNodeToTreeCacheCodec(schemaContext, treeCache).decode(InstanceIdentifier.builder().build(), node);
        wdtt.setSnapshotTree(normalizedNode);


      } else{
          treeCache.getCache().getAdvancedCache().getTransactionManager().begin();
      }
      return treeCache.getCache().getAdvancedCache().getTransactionManager().getTransaction();
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
          Node node = treeCache.getNode(Fqn.fromString(path.toString()));
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
              logger.error("Failed to read : " + e.getMessage());
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

    public WriteAction(SchemaContext schemaContext, TreeCache treeCache, InstanceIdentifier path, NormalizedNode<?, ?> normalizedNode, final WriteDeleteTransactionTracker transactionLog) {
      this.schemaContext = schemaContext;
      this.treeCache = treeCache;
      this.path = path;
      this.normalizedNode = normalizedNode;
      this.transactionLog = transactionLog;
    }

    @Override
    public Object call() throws Exception {
      new NormalizedNodeToTreeCacheCodec(schemaContext, treeCache).encode(path, normalizedNode, transactionLog);
      return null;
    }
  }

  private static class DeleteAction implements Callable {

    private final TreeCache treeCache;
    private final InstanceIdentifier path;

    private final SchemaContext schemaContext;
    private final WriteDeleteTransactionTracker transactionLog;

    public DeleteAction(SchemaContext schemaContext, TreeCache treeCache, InstanceIdentifier path, WriteDeleteTransactionTracker transactionLog) {

      this.treeCache = treeCache;
      this.path = path;
      this.transactionLog = transactionLog;
      this.schemaContext = schemaContext;
    }

    @Override
    public Object call() throws Exception {
      Fqn removeFqn = Fqn.fromString(path.toString());
      NormalizedNode<?, ?> trackRemovedNode = new NormalizedNodeToTreeCacheCodec(schemaContext, treeCache).decode(path, treeCache.getNode(removeFqn));
      treeCache.removeNode(removeFqn);
      transactionLog.track(path.toString(), WriteDeleteTransactionTracker.Operation.REMOVED, trackRemovedNode);
      return null;
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
      listenerTasks = lrm.prepareNotifyTasks(transactionLog);
      return null;
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
          logger.info("COMMITTING TRANSACTION : {}", getIdentifier());
          final ListenableFuture future = commitExecutor.submit(createInfinispanTransactionWrapper(new CommitTransactionAction(treeCache)));
          //TODO : Use a different executor than the crud executor for sending notifications
          future.addListener(new Runnable() {
              @Override
              public void run() {
                  // here we need to call the notify listeners
                  lrm.notifyListeners(listenerTasks);
              } }, crudExecutor);
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
                if(ReadWriteTransactionActor.this.transaction != null){
                    transactionManager.resume(ReadWriteTransactionActor.this.transaction);
                }
                return wrappedCallable.call();
            } catch(Exception e){
                logger.error("Wrapped Call failed", e);
                throw e;
            } finally {
                transactionManager.suspend();
            }
       }
  }

}
