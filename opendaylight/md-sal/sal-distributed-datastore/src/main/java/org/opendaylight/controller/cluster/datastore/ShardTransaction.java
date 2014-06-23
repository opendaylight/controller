/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.DeleteData;
import org.opendaylight.controller.cluster.datastore.messages.DeleteDataReply;
import org.opendaylight.controller.cluster.datastore.messages.MergeData;
import org.opendaylight.controller.cluster.datastore.messages.MergeDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.WriteData;
import org.opendaylight.controller.cluster.datastore.messages.WriteDataReply;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.util.concurrent.ExecutionException;

/**
 * The ShardTransaction Actor represents a remote transaction
 *
 * The ShardTransaction Actor delegates all actions to DOMDataReadWriteTransaction
 *
 * Even though the DOMStore and the DOMStoreTransactionChain implement multiple types of transactions
 * the ShardTransaction Actor only works with read-write transactions. This is just to keep the logic simple. At this
 * time there are no known advantages for creating a read-only or write-only transaction which may change over time
 * at which point we can optimize things in the distributed store as well.
 *
 * Handles Messages
 * ----------------
 * {@link org.opendaylight.controller.cluster.datastore.messages.ReadData}
 * {@link org.opendaylight.controller.cluster.datastore.messages.WriteData}
 * {@link org.opendaylight.controller.cluster.datastore.messages.MergeData}
 * {@link org.opendaylight.controller.cluster.datastore.messages.DeleteData}
 * {@link org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction}
 * {@link org.opendaylight.controller.cluster.datastore.messages.CloseTransaction}
 */
public class ShardTransaction extends UntypedActor {

  private final DOMStoreReadWriteTransaction transaction;

  private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  public ShardTransaction(DOMStoreReadWriteTransaction transaction) {
    this.transaction = transaction;
  }


  public static Props props(final DOMStoreReadWriteTransaction transaction){
    return Props.create(new Creator<ShardTransaction>(){

      @Override
      public ShardTransaction create() throws Exception {
        return new ShardTransaction(transaction);
      }
    });
  }

  @Override
  public void onReceive(Object message) throws Exception {
    if(message instanceof ReadData){
      readData((ReadData) message);
    } else if(message instanceof WriteData){
      writeData((WriteData) message);
    } else if(message instanceof MergeData){
      mergeData((MergeData) message);
    } else if(message instanceof DeleteData){
      deleteData((DeleteData) message);
    } else if(message instanceof ReadyTransaction){
      readyTransaction((ReadyTransaction) message);
    } else if(message instanceof CloseTransaction){
      closeTransaction((CloseTransaction) message);
    }
  }

  private void readData(ReadData message) {
    final ActorRef sender = getSender();
    final ActorRef self = getSelf();
    final InstanceIdentifier path = message.getPath();
    final ListenableFuture<Optional<NormalizedNode<?, ?>>> future = transaction.read(path);

    future.addListener(new Runnable() {
      @Override
      public void run() {
        try {
          Optional<NormalizedNode<?, ?>> optional = future.get();
          if(optional.isPresent()){
            sender.tell(new ReadDataReply(optional.get()), self);
          } else {
            //TODO : Need to decide what to do here
          }
        } catch (InterruptedException | ExecutionException e) {
          log.error(e, "An exception happened when reading data from path : " + path.toString());
        }

      }
    }, getContext().dispatcher());
  }


  private void writeData(WriteData message){
    transaction.write(message.getPath(), message.getData());
    getSender().tell(new WriteDataReply(), getSelf());
  }

  private void mergeData(MergeData message){
    transaction.merge(message.getPath(), message.getData());
    getSender().tell(new MergeDataReply(), getSelf());
  }

  private void deleteData(DeleteData message){
    transaction.delete(message.getPath());
    getSender().tell(new DeleteDataReply(), getSelf());
  }

  private void readyTransaction(ReadyTransaction message){
    DOMStoreThreePhaseCommitCohort cohort = transaction.ready();
    ActorRef cohortActor = getContext().actorOf(ThreePhaseCommitCohort.props(cohort));
    getSender().tell(new ReadyTransactionReply(cohortActor.path()), getSelf());

  }

  private void closeTransaction(CloseTransaction message){
    transaction.close();
    getSender().tell(new CloseTransactionReply(), getSelf());
  }
}
