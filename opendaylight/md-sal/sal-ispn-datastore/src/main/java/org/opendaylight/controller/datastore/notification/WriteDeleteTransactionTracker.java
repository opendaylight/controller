package org.opendaylight.controller.datastore.notification;

import org.opendaylight.controller.datastore.infinispan.utils.NodeIdentifierFactory;
import org.opendaylight.controller.datastore.infinispan.utils.NormalizedNodeNavigator;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author: syedbahm
 * Date: 4/15/14
 */
public class WriteDeleteTransactionTracker {

  final private long transactionNo;

  public enum Operation {CREATED, VISITED, UPDATED, REMOVED}

  private static final Logger LOG = LoggerFactory.getLogger(WriteDeleteTransactionTracker.class);
  //actual one
  private NormalizedNode<?, ?> snapshotTree = null;

  private volatile boolean locked = false;

  private TreeMap<String, TransactionLog> transactionLog = new TreeMap<>();

  private TreeMap<String, SortedMap<String, TransactionLog>> filteredSubTrees = new TreeMap();

  WriteDeleteTransactionTracker snapshotTransactions = null;


  private WriteDeleteTransactionTracker(){
        transactionNo = 0;
  }
  public WriteDeleteTransactionTracker(long transactionNo) {
    this.transactionNo = transactionNo;
  }


  public synchronized void track(String fqnPath, Operation operation, NormalizedNode<?, ?> node) {
    if (!locked) {
      //building the instance identifier
      //spliting this and creating list
      List<InstanceIdentifier.PathArgument> pathArguments = new ArrayList<>();
      String[] ids = fqnPath.split("/");
      for (String nodeId : ids) {
        if (!"".equals(nodeId)) {
          pathArguments.add(NodeIdentifierFactory.getArgument(nodeId));
        }
      }
      //creating instance identifier
      final InstanceIdentifier instanceIdentifier = new InstanceIdentifier(pathArguments);
      transactionLog.put(fqnPath, new TransactionLog(fqnPath,instanceIdentifier, operation, node));
    } else {
      LOG.error("transaction log update after the transaction is locked? {},{},{}", fqnPath, operation, node);
    }
  }

  @Override
  public String toString() {
    return "WriteDeleteTransactionTracker{" +
        "transactionLog=" + transactionLog +
        '}';
  }

  public void setSnapshotTree(NormalizedNode<?, ?> snapshotTree) {
    if (this.snapshotTree == null) {
      this.snapshotTree = snapshotTree;
    } else {
      LOG.warn("There shouldn't be another snapshot set for the same transaction!!!");
    }
  }

  public synchronized void lockTransactionLog() {
    locked = true;
  }

  public synchronized void clear() {
    transactionLog.clear();
  }

  public synchronized boolean isLocked() {
    return locked;
  }

  public Set<String> filterTransactionsPaths(Set<String> instanceIdentifiers) {
    //ok here we will prepare the subtrees for all interested transactions
    filteredSubTrees = new TreeMap<>();

    printTree(transactionLog);

    for (String id : instanceIdentifiers) {
      LOG.debug("Registered listener for path {}", id);
      //let us check if its present in the transactionLog
      if (transactionLog.containsKey(id)) {
        filteredSubTrees.put(id, transactionLog.tailMap(id));
      }
    }
    if (!filteredSubTrees.isEmpty()) {
      //let us prepare a transaction log of the snapshot items
      prepareSnapshotTransactionLog();
    } else {
      LOG.debug("No registered listener found");
    }
    return filteredSubTrees.keySet();
  }

  private void printTree(TreeMap<String, TransactionLog> transactionLog) {
    for(Map.Entry<String,TransactionLog>entry:transactionLog.entrySet()){
         System.out.println("----");
         System.out.println(entry.getKey());
         System.out.println(entry.getValue().getInstanceIdentifier()) ;
         System.out.println("----");
    }
  }

  private void prepareSnapshotTransactionLog() {
    if(snapshotTree != null) {
      String parentPath = "/";
      snapshotTransactions = new WriteDeleteTransactionTracker();
      new NormalizedNodeNavigator(new SnapshotNormalizedNodeTransactionLogMapper(snapshotTransactions)).navigate(parentPath, snapshotTree);
    }else{
      LOG.warn("Snapshot tree was found to be empty for the transaction " + transactionNo);
    }
  }

  public DOMImmutableDataChangeEvent prepareEvent(String path, Boolean[] resultScope) {
    //ok here we will check that the filteredTrees have been prepared
    checkState(filteredSubTrees != null, "The transactions haven't been filtered");


    DOMImmutableDataChangeEvent.Builder builder = DOMImmutableDataChangeEvent.builder(AsyncDataBroker.DataChangeScope.BASE);
    SortedMap<String, TransactionLog> filteredSubTree = filteredSubTrees.get(path);

    if ((filteredSubTree == null) || filteredSubTree.isEmpty()) {


      resultScope[0]=true;
      //seems its a leaf transaction
      snapshotTransactions.resolveLeaf(path, transactionLog.get(path), builder);


    } else {
      //need to determine the ImmutableDataChangeEvent
      snapshotTransactions.resolve(path, filteredSubTree, builder,resultScope);

      //ok here we need to get the snapshot at path level the new one.
      //TODO: check that the normalize node will have all the details of the children?
      TransactionLog newOne = transactionLog.get(path);
      if ((newOne != null)) {
        builder.setAfter(newOne.getNode());
      }
    }

    return builder.build();
  }

  private boolean resolveLeaf(String path,
                            TransactionLog newOne,
                            DOMImmutableDataChangeEvent.Builder builder
                            ) {
    boolean changed = true;
    TransactionLog oldOne = transactionLog.get(path);

    if (oldOne == null) {
      LOG.debug("Node was added at path {}", path);
      builder.addCreated(newOne.getInstanceIdentifier(), newOne.getNode());

    } else if (newOne.getOp() == Operation.REMOVED) {
      LOG.debug("Node was added at path {}", path);
      builder.addRemoved(newOne.getInstanceIdentifier(), oldOne.getNode());

    } else if ((newOne.getOp() == Operation.UPDATED) && (oldOne.getOp() == Operation.VISITED)) {
      LOG.debug("Node was added at path {}", path);
      builder.addUpdated(newOne.getInstanceIdentifier(), oldOne.getNode(), newOne.getNode());

    } else if ((newOne.getOp() == Operation.VISITED) && (oldOne.getOp() == Operation.VISITED)) {
      LOG.debug("Not change found at the node level {} - before and after", path);
      changed = false;
    }
    return changed;
  }

  private void resolve(String path, SortedMap<String, TransactionLog> potentials, DOMImmutableDataChangeEvent.Builder builder, Boolean[] resultScope) {

    //here we need to get the SortedMap of the path in snapshotTransactions
    SortedMap<String, TransactionLog> existing = transactionLog.tailMap(path);
    if ((existing == null || existing.isEmpty())) {
      //seems leaf node?
      if(transactionLog.get(path) != null){
           resolveLeaf(path,potentials.get(path),builder);
      }else {
        if (potentials.size() <= 2) {
          resultScope[1] = true;
        } else {
          resultScope[2] = true;
        }
        //all the elements in the potentials are added?
        for (Map.Entry<String, TransactionLog> potential : potentials.entrySet()) {
          builder.addCreated(potential.getValue().getInstanceIdentifier(), potential.getValue().getNode());
          if (potential.getKey().equals(path)) {
            resultScope[0] = true;
          }
        }
      }
    } else {
      //TODO:normalize node will have all the details of the children?
      TransactionLog oldOne = transactionLog.get(path);
      if ((oldOne != null)) {
        builder.setBefore(oldOne.getNode());
      }

      //we assume that the potential changes are less hence start looping using it
      for (Map.Entry<String, TransactionLog> potential : potentials.entrySet()) {

        boolean changed = resolveLeaf(potential.getKey(), potential.getValue(), builder);
        if(changed && potential.getKey().equals(path)){
          resultScope[0]=true;
        }
      }
    }
  }

  private TransactionLog getTransactionLog(String fqnPath) {
    return transactionLog.get(fqnPath);
  }


  private static class TransactionLog {


    final private String fqnPath;
    final private  InstanceIdentifier id;
    final private NormalizedNode<?, ?> node;
    final private Operation op;


    private TransactionLog() {
      fqnPath = null;
      id = null;
      node = null;
      op = null;
    }

    public TransactionLog(String fqnPath,InstanceIdentifier id, Operation op, NormalizedNode<?, ?> node) {
      this.fqnPath = fqnPath;
      this.op = op;
      this.node = node;
      this.id = id;
    }

    public String getFqnPath() {
      return fqnPath;
    }

    public NormalizedNode<?, ?> getNode() {
      return node;
    }

    public Operation getOp() {
      return op;
    }
    public InstanceIdentifier getInstanceIdentifier(){
      return id;
    }
  }
}


