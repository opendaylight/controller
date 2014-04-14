package org.opendaylight.controller.datastore.notification;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.TreeMap;

/**
 * @author: syedbahm
 * Date: 4/15/14
 */
public class WriteDeleteTransactionTracker {

  public enum Operation {CREATED,UPDATED,REMOVED};
  private static final Logger LOG = LoggerFactory.getLogger(WriteDeleteTransactionTracker.class);
  private NormalizedNode<?,?>snapshotTree = null;
  private volatile boolean locked =false;

  TreeMap<String, TransactionLog> transactionLog = new TreeMap<>();

  public synchronized void track(String fqnPath,Operation operation,NormalizedNode<?,?> node){
     if(!locked){
     transactionLog.put(fqnPath, new TransactionLog(fqnPath, operation, node));
     }else{
       LOG.error("transaction log update after the transaction is locked? {},{},{}",fqnPath,operation,node);
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

  public WriteDeleteTransactionTracker preparePotentialTasks(Set<String> instanceIdentifiers) {
      for(String id: instanceIdentifiers){
        //let us check if its present in the transactionLog
        transactionLog.containsKey(id);
      }
      return this;
  }

  private static class TransactionLog{


    final private  String fqnPath;
    //final private  InstanceIdentifier.NodeIdentifier nodeId;
    final private  NormalizedNode<?,?> node;
    final private  Operation op;

    private TransactionLog(){
           fqnPath=null;
           //nodeId= null;
           node = null;
           op = null;
    }

    public TransactionLog(String fqnPath, Operation op, NormalizedNode<?, ?> node){
      this.fqnPath = fqnPath;
      this.op = op;
      this.node = node;
    }

  }
}


