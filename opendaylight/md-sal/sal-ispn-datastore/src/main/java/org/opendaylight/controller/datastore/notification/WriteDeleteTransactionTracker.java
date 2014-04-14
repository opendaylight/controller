package org.opendaylight.controller.datastore.notification;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author: syedbahm
 * Date: 4/15/14
 */
public class WriteDeleteTransactionTracker {

  public enum Operation {CREATED,UPDATED,REMOVED};
  private static final Logger LOG = LoggerFactory.getLogger(WriteDeleteTransactionTracker.class);
  //actual one
  private NormalizedNode<?,?>snapshotTree = null;

  private volatile boolean locked =false;

  private  TreeMap<String, TransactionLog> transactionLog = new TreeMap<>();
  private Map<String,SortedMap<String,TransactionLog>> subTrees = null;
  private TreeMap<String,TransactionLog>baseNotifiable = new TreeMap<> ();
  private TreeMap<String,SortedMap<String,TransactionLog>>firstLevelNotifiable = new TreeMap<>();
  private TreeMap<String,SortedMap<String,TransactionLog>>filteredSubTrees =  new TreeMap();


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
    //ok here we will prepare the subtrees for all interested transactions
    subTrees = new HashMap<>();

    for(String id: instanceIdentifiers){
      //let us check if its present in the transactionLog
      if(transactionLog.containsKey(id)){
        subTrees.put(id,transactionLog.tailMap(id));
      }
    }

    //ok here we will prepare the subtree modifications
    //let us go over the base level items -- aka the ones that have null sorted maps :)
    for(Map.Entry<String,SortedMap<String,TransactionLog>>entry:subTrees.entrySet()){
      if(entry.getValue()== null){
        baseNotifiable.put(entry.getKey(),transactionLog.get(entry.getKey()));
      }else if (entry.getValue().size() <= 2){  //first level items

      }
    }
    return this;
  }



  void trackTest(String fqnPath,Operation operation,NormalizedNode<?,?> node){
    if(!locked){
      transactionLog.put(fqnPath, new TransactionLog(fqnPath, operation, node));
    }else{
      LOG.error("transaction log update after the transaction is locked? {},{},{}",fqnPath,operation,node);
    }
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


