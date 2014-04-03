package org.opendaylight.controller.datastore.infinispan;

import com.google.common.base.Preconditions;
import org.infinispan.tree.TreeCache;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

public abstract class AbstractDOMStoreTransaction implements InfinispanDOMStoreTransaction{
    private final String transactionId;
    private final SchemaContext schemaContext;
    private final TreeCache treeCache;
    private final Transaction transaction;
    private final TransactionManager transactionManager;

    protected AbstractDOMStoreTransaction(long transactionNo, SchemaContext schemaContext, TreeCache treeCache){
        Preconditions.checkNotNull(schemaContext, "schemaContext should not be null");
        Preconditions.checkNotNull(treeCache, "treeCache should not be null");

        this.schemaContext = schemaContext;
        this.treeCache = treeCache;
        transactionId = "ispn-txn-" + transactionNo;
        transactionManager = treeCache.getCache().getAdvancedCache().getTransactionManager();

        try {
            transactionManager.begin();
            transaction = transactionManager.getTransaction();
        } catch (NotSupportedException e) {
            throw new RuntimeException(e);
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    public Object getIdentifier() {
        return transactionId;
    }

    public void close() {
    }

    protected SchemaContext getSchemaContext(){
        return schemaContext;
    }

    protected TreeCache getTreeCache(){
        return treeCache;
    }

    public void resumeWrappedTransaction(){
        try {
            transactionManager.resume(transaction);
        } catch (InvalidTransactionException e) {
            throw new RuntimeException(e);
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    public void commitWrappedTransaction(){
        try {
            transactionManager.commit();
        } catch (RollbackException e) {
            throw new RuntimeException(e);
        } catch (HeuristicMixedException e) {
            throw new RuntimeException(e);
        } catch (HeuristicRollbackException e) {
            throw new RuntimeException(e);
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }
}
