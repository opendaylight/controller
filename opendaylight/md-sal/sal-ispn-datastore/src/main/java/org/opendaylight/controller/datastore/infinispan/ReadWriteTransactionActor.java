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

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class ReadWriteTransactionActor implements DOMStoreReadWriteTransaction, AutoCloseable{

    private final ListeningExecutorService executor;
    private final SchemaContext schemaContext;
    private final TreeCache treeCache;
    private final String transactionId;
    private final WriteDeleteTransactionTracker transactionLog;
    private ListenerRegistrationManager listenerManager = null;


    public ReadWriteTransactionActor(SchemaContext schemaContext, TreeCache treeCache, ListeningExecutorService executor, long transactionNo){
        this.schemaContext = schemaContext;
        this.treeCache = treeCache;
        this.executor = executor;
        this.transactionId = "ispn-txn-" + transactionNo;
        transactionLog = new WriteDeleteTransactionTracker();
        executor.submit(new BeginTransactionAction(treeCache,schemaContext,transactionLog));
    }

    @Override
    public Object getIdentifier() {
        return this.transactionId;
    }

    @Override
    public void close(){
        final ListenableFuture future = executor.submit(new RollbackTransactionAction(treeCache,transactionLog));
        try {
            future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        executor.shutdown();
    }

    @Override
    public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(InstanceIdentifier path) {
        if(path != null){
            System.out.println("Read : " + path.toString());
        } else {
            System.out.println("Reading from null path");
        }
        final ListenableFuture future = executor.submit(new ReadAction(schemaContext, treeCache, path));
        return Futures.transform(future, new Function<NormalizedNode<?,?>, Optional< NormalizedNode <?,?>>>() {
            @Nullable
            @Override
            public Optional<NormalizedNode<?, ?>> apply(@Nullable NormalizedNode<?,?> o) {
                final Optional<NormalizedNode<?, ?>> of = Optional.<NormalizedNode<?,?>>of(o);
                return of;
            }
        });
    }

    @Override
    public void write(InstanceIdentifier path, NormalizedNode<?, ?> data) {
        if(path != null){
            System.out.println("Write : " + path.toString());
        } else {
            System.out.println("Writing to null path");
        }

        final ListenableFuture future = executor.submit(new WriteAction(schemaContext, treeCache, path, data,transactionLog));
        try {
            future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(InstanceIdentifier path) {
        final ListenableFuture future = executor.submit(new DeleteAction(schemaContext,treeCache, path,transactionLog));
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
        //TODO: we should seal the transactions here
        transactionLog.lockTransactionLog();

        return new DOMStoreThreePhaseCommitImpl(transactionLog,listenerManager);
    }

  public void setListenerManager(ListenerRegistrationManager listenerManager) {
    this.listenerManager = listenerManager;
  }

  private static class BeginTransactionAction implements Callable {
        public static int INFINISPAN_TRANSACTION_TIMEOUT = 120; //2 minutes;
        private final TreeCache treeCache;
        private final WriteDeleteTransactionTracker wdtt;
        private final SchemaContext schemaContext ;

        public BeginTransactionAction(TreeCache treeCache, SchemaContext sc, final WriteDeleteTransactionTracker wdt){
            this.treeCache = treeCache;
            this.wdtt = wdt;
            this.schemaContext = sc;
        }

        @Override
        public Object call() throws Exception {
            treeCache.getCache().getAdvancedCache().getTransactionManager().setTransactionTimeout(INFINISPAN_TRANSACTION_TIMEOUT);
            treeCache.getCache().getAdvancedCache().getTransactionManager().begin();
            Node node = treeCache.getNode(Fqn.fromString("/"));
            final NormalizedNode<?, ?> normalizedNode = new NormalizedNodeToTreeCacheCodec(schemaContext, treeCache).decode(InstanceIdentifier.builder().build(), node);
            wdtt.setSnapshotTree(normalizedNode);
            return null;
        }
    }

    private static class RollbackTransactionAction implements Callable {
        private final TreeCache treeCache;
        private final WriteDeleteTransactionTracker transactionLog;

        public RollbackTransactionAction(TreeCache treeCache, WriteDeleteTransactionTracker transactionLog){
            this.treeCache = treeCache;
            this.transactionLog = transactionLog;
        }

        @Override
        public Object call() throws Exception {
            treeCache.getCache().getAdvancedCache().getTransactionManager().rollback();
            transactionLog.clear();
            return null;
        }
    }

    private static class CommitTransactionAction implements Callable {
        private final TreeCache treeCache;


        public CommitTransactionAction(TreeCache treeCache){
            this.treeCache = treeCache;
        }

        @Override
        public Object call() throws Exception {
            treeCache.getCache().getAdvancedCache().getTransactionManager().commit();
            return null;
        }
    }

    private static class ReadAction implements Callable<NormalizedNode<?,?>> {
        private final TreeCache treeCache;
        private final InstanceIdentifier path;
        private final SchemaContext schemaContext;

        public ReadAction(SchemaContext schemaContext, TreeCache treeCache, InstanceIdentifier path){
            this.schemaContext = schemaContext;
            this.treeCache = treeCache;
            this.path = path;
        }

        @Override
        public NormalizedNode<?,?> call() throws Exception {
            Node node = treeCache.getNode(Fqn.fromString(path.toString()));
            final NormalizedNode<?, ?> normalizedNode = new NormalizedNodeToTreeCacheCodec(schemaContext, treeCache).decode(path, node);
            return normalizedNode;
        }
    }

    private static class WriteAction implements Callable {

        private final TreeCache treeCache;
        private final InstanceIdentifier path;
        private final NormalizedNode<?, ?> normalizedNode;
        private final SchemaContext schemaContext;
        private final WriteDeleteTransactionTracker transactionLog;

        public WriteAction(SchemaContext schemaContext, TreeCache treeCache, InstanceIdentifier path, NormalizedNode<?,?> normalizedNode,final WriteDeleteTransactionTracker transactionLog){
            this.schemaContext = schemaContext;
            this.treeCache = treeCache;
            this.path = path;
            this.normalizedNode = normalizedNode;
            this.transactionLog = transactionLog;
        }

        @Override
        public Object call() throws Exception {
            new NormalizedNodeToTreeCacheCodec(schemaContext, treeCache).encode(path, normalizedNode,transactionLog);
            return null;
        }
    }

    private static class DeleteAction implements Callable {

        private final TreeCache treeCache;
        private final InstanceIdentifier path;

        private final SchemaContext schemaContext;
        private final WriteDeleteTransactionTracker transactionLog;

        public DeleteAction(SchemaContext schemaContext,TreeCache treeCache, InstanceIdentifier path,WriteDeleteTransactionTracker transactionLog){

            this.treeCache = treeCache;
            this.path = path;
            this.transactionLog = transactionLog;
            this.schemaContext = schemaContext;
        }
        @Override
        public Object call() throws Exception {
            Fqn removeFqn = Fqn.fromString(path.toString());
            NormalizedNode<?,?> trackRemovedNode =  new NormalizedNodeToTreeCacheCodec(schemaContext, treeCache).decode(path, treeCache.getNode(removeFqn));
            treeCache.removeNode(removeFqn);
            transactionLog.track(path.toString(), WriteDeleteTransactionTracker.Operation.REMOVED,trackRemovedNode);
            return null;
        }
    }


  private class DOMStoreThreePhaseCommitImpl implements DOMStoreThreePhaseCommitCohort {

    private final WriteDeleteTransactionTracker transactionLog;
    private final ListenerRegistrationManager lrm;



    private List<ChangeListenerNotifyTask> listenerTasks;

    public DOMStoreThreePhaseCommitImpl(final WriteDeleteTransactionTracker transactionLog,ListenerRegistrationManager lrm) {
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
      final ListenableFuture future = executor.submit(new RollbackTransactionAction(treeCache, transactionLog));
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


      ListenableFuture future = executor.submit(new CommitTransactionAction(treeCache));
      future.addListener(new Runnable() {
        @Override
        public void run() {

            // here we need to call the notify listeners
            lrm.notifyListeners(listenerTasks);


        }
      },executor );
      return Futures.transform(future, new Function<Object, Void>() {

        @Nullable
        @Override
        public Void apply(@Nullable Object o) {
          return null;
        }
      });
    }

  }

}
