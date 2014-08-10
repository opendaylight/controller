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
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.cluster.datastore.exceptions.UnknownMessageException;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.DataExistsReply;
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
import org.opendaylight.controller.cluster.datastore.modification.CompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.ImmutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import java.util.concurrent.ExecutionException;

/**
 * The ShardTransaction Actor represents a remote transaction
 * <p>
 * The ShardTransaction Actor delegates all actions to DOMDataReadWriteTransaction
 * </p>
 * <p>
 * Even though the DOMStore and the DOMStoreTransactionChain implement multiple types of transactions
 * the ShardTransaction Actor only works with read-write transactions. This is just to keep the logic simple. At this
 * time there are no known advantages for creating a read-only or write-only transaction which may change over time
 * at which point we can optimize things in the distributed store as well.
 * </p>
 * <p>
 * Handles Messages <br/>
 * ---------------- <br/>
 * <li> {@link org.opendaylight.controller.cluster.datastore.messages.ReadData}
 * <li> {@link org.opendaylight.controller.cluster.datastore.messages.WriteData}
 * <li> {@link org.opendaylight.controller.cluster.datastore.messages.MergeData}
 * <li> {@link org.opendaylight.controller.cluster.datastore.messages.DeleteData}
 * <li> {@link org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction}
 * <li> {@link org.opendaylight.controller.cluster.datastore.messages.CloseTransaction}
 * </p>
 */
public abstract class ShardTransaction extends AbstractUntypedActor {

  private final ActorRef shardActor;
  protected final SchemaContext schemaContext;

  // FIXME : see below
  // If transactionChain is not null then this transaction is part of a
  // transactionChain. Not really clear as to what that buys us
  private final DOMStoreTransactionChain transactionChain;


  private final MutableCompositeModification modification =
      new MutableCompositeModification();

  private final LoggingAdapter log =
      Logging.getLogger(getContext().system(), this);

  protected ShardTransaction(
                          ActorRef shardActor, SchemaContext schemaContext) {
    this(null,  shardActor, schemaContext);
  }

  protected ShardTransaction(DOMStoreTransactionChain transactionChain,
                          ActorRef shardActor, SchemaContext schemaContext) {
    this.transactionChain = transactionChain;
    this.shardActor = shardActor;
    this.schemaContext = schemaContext;
  }



  public static Props props(final DOMStoreReadTransaction transaction,
                            final ActorRef shardActor, final SchemaContext schemaContext) {
    return Props.create(new Creator<ShardTransaction>() {

      @Override
      public ShardTransaction create() throws Exception {
        return new ShardReadTransaction(transaction, shardActor, schemaContext);
      }
    });
  }

  public static Props props(final DOMStoreTransactionChain transactionChain, final DOMStoreReadTransaction transaction,
                            final ActorRef shardActor, final SchemaContext schemaContext) {
    return Props.create(new Creator<ShardTransaction>() {

      @Override
      public ShardTransaction create() throws Exception {
        return new ShardReadTransaction(transactionChain, transaction, shardActor, schemaContext);
      }
    });
  }

  public static Props props(final DOMStoreReadWriteTransaction transaction,
                            final ActorRef shardActor, final SchemaContext schemaContext) {
    return Props.create(new Creator<ShardTransaction>() {

      @Override
      public ShardTransaction create() throws Exception {
        return new ShardReadWriteTransaction(transaction, shardActor, schemaContext);
      }
    });
  }

  public static Props props(final DOMStoreTransactionChain transactionChain, final DOMStoreReadWriteTransaction transaction,
                            final ActorRef shardActor, final SchemaContext schemaContext) {
    return Props.create(new Creator<ShardTransaction>() {

      @Override
      public ShardTransaction create() throws Exception {
        return new ShardReadWriteTransaction(transactionChain, transaction, shardActor, schemaContext);
      }
    });
  }


  public static Props props(final DOMStoreWriteTransaction transaction,
                            final ActorRef shardActor, final SchemaContext schemaContext) {
    return Props.create(new Creator<ShardTransaction>() {

      @Override
      public ShardTransaction create() throws Exception {
        return new ShardWriteTransaction(transaction, shardActor, schemaContext);
      }
    });
  }

  public static Props props(final DOMStoreTransactionChain transactionChain, final DOMStoreWriteTransaction transaction,
                            final ActorRef shardActor, final SchemaContext schemaContext) {
    return Props.create(new Creator<ShardTransaction>() {

      @Override
      public ShardTransaction create() throws Exception {
        return new ShardWriteTransaction(transactionChain, transaction, shardActor, schemaContext);
      }
    });
  }


  @Override
  public void handleReceive(Object message) throws Exception {
     if (message.getClass().equals(CloseTransaction.SERIALIZABLE_CLASS)) {
      closeTransaction(new CloseTransaction());
    } else if (message instanceof GetCompositedModification) {
      // This is here for testing only
      getSender().tell(new GetCompositeModificationReply(
          new ImmutableCompositeModification(modification)), getSelf());
    }else{
         throw new UnknownMessageException(message);
    }
  }

  abstract protected  void closeTransaction(CloseTransaction message);

  protected void readData(DOMStoreReadTransaction transaction,ReadData message) {
    final ActorRef sender = getSender();
    final ActorRef self = getSelf();
    final YangInstanceIdentifier path = message.getPath();
    final ListenableFuture<Optional<NormalizedNode<?, ?>>> future =
        transaction.read(path);

    future.addListener(new Runnable() {
      @Override
      public void run() {
        try {
          Optional<NormalizedNode<?, ?>> optional = future.get();
          if (optional.isPresent()) {
            sender.tell(new ReadDataReply(schemaContext,optional.get()).toSerializable(), self);
          } else {
            sender.tell(new ReadDataReply(schemaContext,null).toSerializable(), self);
          }
        } catch (InterruptedException | ExecutionException e) {
          log.error(e,
              "An exception happened when reading data from path : "
                  + path.toString());
        }

      }
    }, getContext().dispatcher());
  }

    protected void dataExists(DOMStoreReadTransaction transaction, DataExists message) {
        final YangInstanceIdentifier path = message.getPath();



        try {
            Boolean exists = transaction.exists(path).checkedGet();
            getSender().tell(new DataExistsReply(exists), getSelf());
        } catch (ReadFailedException e) {
            // Do nothing here. This will result in a timeout on the caller end
            // Basheeruddin is working on a strategy to transport the exception
            // to the caller
        }

    }

  protected void writeData(DOMStoreWriteTransaction transaction, WriteData message) {
    modification.addModification(
        new WriteModification(message.getPath(), message.getData(),schemaContext));
    LOG.debug("writeData at path : " + message.getPath().toString());
    transaction.write(message.getPath(), message.getData());
    getSender().tell(new WriteDataReply().toSerializable(), getSelf());
  }

  protected void mergeData(DOMStoreWriteTransaction transaction, MergeData message) {
    modification.addModification(
        new MergeModification(message.getPath(), message.getData(), schemaContext));
    LOG.debug("mergeData at path : " + message.getPath().toString());
    transaction.merge(message.getPath(), message.getData());
    getSender().tell(new MergeDataReply().toSerializable(), getSelf());
  }

  protected void deleteData(DOMStoreWriteTransaction transaction, DeleteData message) {
    LOG.debug("deleteData at path : " + message.getPath().toString());
    modification.addModification(new DeleteModification(message.getPath()));
    transaction.delete(message.getPath());
    getSender().tell(new DeleteDataReply().toSerializable(), getSelf());
  }

  protected void readyTransaction(DOMStoreWriteTransaction transaction, ReadyTransaction message) {
    DOMStoreThreePhaseCommitCohort cohort =  transaction.ready();
    ActorRef cohortActor = getContext().actorOf(
        ThreePhaseCommitCohort.props(cohort, shardActor, modification), "cohort");
    getSender()
        .tell(new ReadyTransactionReply(cohortActor.path()).toSerializable(), getSelf());

  }


  // These classes are in here for test purposes only


  static class GetCompositedModification {

  }


  static class GetCompositeModificationReply {
    private final CompositeModification modification;


    GetCompositeModificationReply(CompositeModification modification) {
      this.modification = modification;
    }


    public CompositeModification getModification() {
      return modification;
    }
  }
}
