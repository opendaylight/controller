package org.opendaylight.controller.datastore.notification;

import org.infinispan.tree.Fqn;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: syedbahm
 * Date: 4/15/14
 */
public class WriteDeleteTransactionTracker {

  public enum Operation {CREATED,UPDATED,REMOVED};
  private static final Logger LOG = LoggerFactory.getLogger(WriteDeleteTransactionTracker.class);
  private NormalizedNode<?,?>snapshotTree = null;
  private volatile boolean locked =false;

  Map<Fqn, TransactionLog> transactionLog = new ConcurrentHashMap<>();

  public synchronized void track(Fqn fqn,Operation operation,NormalizedNode<?,?> node){
     if(!locked){
     transactionLog.put(fqn, new TransactionLog(fqn, operation, node));
     }else{
       LOG.error("transaction log update after the transaction is locked? {},{},{}",fqn,operation,node);
     }
  }

  @Override
  public String toString() {
    return "WriteDeleteTransactionTracker{" +
        "transactionLog=" + transactionLog +
        '}';
  }

  public void setSnapshotTree(NormalizedNode<?, ?> snapshotTree) {
    if(this.snapshotTree==null){
    this.snapshotTree = snapshotTree;
    }else{
       LOG.warn("There shouldn't be another snapshot set for the same transaction!!!");
    }
  }

  public synchronized void lockTransactionLog(){
         locked = true;
  }

  public synchronized void clear(){
    transactionLog.clear();
  }

  public synchronized boolean isLocked() {
    return locked;
  }

  public WriteDeleteTransactionTracker preparePotentialTasks(Set<InstanceIdentifier> instanceIdentifiers) {
      //TODO: PREPARE THE TASKS BASED ON REGISTERED LISTENERS
      return this;
  }

  private static class TransactionLog{


    final private  Fqn fqn;
    //final private  InstanceIdentifier.NodeIdentifier nodeId;
    final private  NormalizedNode<?,?> node;
    final private  Operation op;

    private TransactionLog(){
           fqn=null;
           //nodeId= null;
           node = null;
           op = null;
    }

    public TransactionLog(Fqn fqn, Operation op, NormalizedNode<?, ?> node){
      this.fqn = fqn;
      this.op = op;
      this.node = node;
    }

  }
}


