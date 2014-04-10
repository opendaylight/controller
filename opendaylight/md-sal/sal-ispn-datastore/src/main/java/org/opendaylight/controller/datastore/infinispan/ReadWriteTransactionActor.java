package org.opendaylight.controller.datastore.infinispan;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.infinispan.tree.Fqn;
import org.infinispan.tree.Node;
import org.infinispan.tree.TreeCache;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class ReadWriteTransactionActor implements DOMStoreReadWriteTransaction, AutoCloseable{

    private final ListeningExecutorService crudExecutor;
    private final ListeningExecutorService commitExecutor;

    private final SchemaContext schemaContext;
    private final TreeCache treeCache;
    private final String transactionId;
    private final Transaction transaction;

    private static final Logger logger = LoggerFactory.getLogger(ReadWriteTransactionActor.class);

    public ReadWriteTransactionActor(SchemaContext schemaContext, TreeCache treeCache,
                                     ListeningExecutorService crudExecutor, ListeningExecutorService commitExecutor,
                                     long transactionNo){
        this.schemaContext = schemaContext;
        this.treeCache = treeCache;
        this.crudExecutor = crudExecutor;
        this.commitExecutor = commitExecutor;
        this.transactionId = "ispn-txn-" + transactionNo;

        final ListenableFuture future = this.crudExecutor.submit(createInfinispanTransactionWrapper(new BeginTransactionAction(treeCache, this.crudExecutor, transactionId)));
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

    public Callable createInfinispanTransactionWrapper(Callable wrappedCallable){
        return new InfinispanTransactionWrapper(wrappedCallable);
    }

    @Override
    public void close(){
        logger.info("CLOSING TRANSACTION : {}", getIdentifier());

//        final ListenableFuture future = crudExecutor.submit(createInfinispanTransactionWrapper(new RollbackTransactionAction(treeCache)));
//        try {
//            future.get();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } catch (ExecutionException e) {
//            throw new RuntimeException(e);
//        }

        //crudExecutor.shutdown();
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

        final ListenableFuture future = crudExecutor.submit(createInfinispanTransactionWrapper(new WriteAction(schemaContext, treeCache, path, data)));
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
        final ListenableFuture future = crudExecutor.submit(createInfinispanTransactionWrapper(new DeleteAction(treeCache, path)));
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
        return new DOMStoreThreePhaseCommitCohort(){

            @Override
            public ListenableFuture<Boolean> canCommit() {
                return Futures.immediateFuture(true);
            }

            @Override
            public ListenableFuture<Void> preCommit() {
                return Futures.immediateFuture(null);
            }

            @Override
            public ListenableFuture<Void> abort() {
                final ListenableFuture future = crudExecutor.submit(createInfinispanTransactionWrapper(new RollbackTransactionAction(treeCache)));
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
                final ListenableFuture future = crudExecutor.submit(createInfinispanTransactionWrapper(new CommitTransactionAction(treeCache)));
                return Futures.transform(future, new Function<Object, Void>() {

                    @Nullable
                    @Override
                    public Void apply(@Nullable Object o) {
                        return null;
                    }
                });

            }
        };
    }

    private static class BeginTransactionAction implements Callable {
        private final TreeCache treeCache;
        private final ListeningExecutorService executor;
        private final Object transactionIdentifier;

        public BeginTransactionAction(TreeCache treeCache, ListeningExecutorService executor, Object transactionIdentifier){
            this.treeCache = treeCache;
            this.executor = executor;
            this.transactionIdentifier = transactionIdentifier;
        }

        @Override
        public Object call() throws Exception {
            treeCache.getCache().getAdvancedCache().getTransactionManager().begin();
            final Transaction transaction = treeCache.getCache().getAdvancedCache().getTransactionManager().getTransaction();
            transaction.registerSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {

                }

                @Override
                public void afterCompletion(int i) {
//                    if (!crudExecutor.isShutdown()) {
//                        logger.info("SHUTDOWN TRANSACTION THREAD : {} ", transactionIdentifier);
//                        crudExecutor.shutdown();
//                    }
                }
            });
            return transaction;
        }
    }

    private static class RollbackTransactionAction implements Callable {
        private final TreeCache treeCache;

        public RollbackTransactionAction(TreeCache treeCache){
            this.treeCache = treeCache;
        }

        @Override
        public Object call() throws Exception {
            final Transaction transaction = treeCache.getCache().getAdvancedCache().getTransactionManager().getTransaction();
            if(transaction != null){
                treeCache.getCache().getAdvancedCache().getTransactionManager().rollback();
            }
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

    private static class ReadAction implements Callable<Object> {
        private final TreeCache treeCache;
        private final InstanceIdentifier path;
        private final SchemaContext schemaContext;

        public ReadAction(SchemaContext schemaContext, TreeCache treeCache, InstanceIdentifier path){
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

        public WriteAction(SchemaContext schemaContext, TreeCache treeCache, InstanceIdentifier path, NormalizedNode<?,?> normalizedNode){
            this.schemaContext = schemaContext;
            this.treeCache = treeCache;
            this.path = path;
            this.normalizedNode = normalizedNode;
        }

        @Override
        public Object call() throws Exception {
            new NormalizedNodeToTreeCacheCodec(schemaContext, treeCache).encode(path, normalizedNode);
            return null;
        }
    }

    private static class DeleteAction implements Callable {

        private final TreeCache treeCache;
        private final InstanceIdentifier path;

        public DeleteAction(TreeCache treeCache, InstanceIdentifier path){

            this.treeCache = treeCache;
            this.path = path;
        }
        @Override
        public Object call() throws Exception {
            try {
                treeCache.removeNode(Fqn.fromString(path.toString()));
            } catch(Exception e){
                logger.error("Failed to delete : " + e.getMessage());
                logger.trace("More info", e);
            }
            return null;
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
