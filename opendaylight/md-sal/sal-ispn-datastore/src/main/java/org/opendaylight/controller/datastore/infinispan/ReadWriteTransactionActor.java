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

import javax.annotation.Nullable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class ReadWriteTransactionActor implements DOMStoreReadWriteTransaction, AutoCloseable{

    private final ListeningExecutorService executor;
    private final SchemaContext schemaContext;
    private final TreeCache treeCache;
    private final String transactionId;

    public ReadWriteTransactionActor(SchemaContext schemaContext, TreeCache treeCache, ListeningExecutorService executor, long transactionNo){
        this.schemaContext = schemaContext;
        this.treeCache = treeCache;
        this.executor = executor;
        this.transactionId = "ispn-txn-" + transactionNo;

        executor.submit(new BeginTransactionAction(treeCache));
    }

    @Override
    public Object getIdentifier() {
        return this.transactionId;
    }

    @Override
    public void close(){
        final ListenableFuture future = executor.submit(new RollbackTransactionAction(treeCache));
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

        final ListenableFuture future = executor.submit(new WriteAction(schemaContext, treeCache, path, data));
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
        final ListenableFuture future = executor.submit(new DeleteAction(treeCache, path));
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
                final ListenableFuture future = executor.submit(new RollbackTransactionAction(treeCache));
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
                final ListenableFuture future = executor.submit(new CommitTransactionAction(treeCache));
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

        public BeginTransactionAction(TreeCache treeCache){
            this.treeCache = treeCache;
        }

        @Override
        public Object call() throws Exception {
            treeCache.getCache().getAdvancedCache().getTransactionManager().begin();
            return null;
        }
    }

    private static class RollbackTransactionAction implements Callable {
        private final TreeCache treeCache;

        public RollbackTransactionAction(TreeCache treeCache){
            this.treeCache = treeCache;
        }

        @Override
        public Object call() throws Exception {
            treeCache.getCache().getAdvancedCache().getTransactionManager().rollback();
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
            treeCache.removeNode(Fqn.fromString(path.toString()));
            return null;
        }
    }

}
