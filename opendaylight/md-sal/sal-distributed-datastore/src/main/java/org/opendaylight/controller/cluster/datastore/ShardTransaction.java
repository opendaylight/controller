/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
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
import org.opendaylight.controller.cluster.datastore.modification.CompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.ImmutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

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
public class ShardTransaction extends AbstractUntypedActor {

    private final ActorRef shardActor;

    // FIXME : see below
    // If transactionChain is not null then this transaction is part of a
    // transactionChain. Not really clear as to what that buys us
    private final DOMStoreTransactionChain transactionChain;

    private final DOMStoreReadWriteTransaction transaction;

    private final MutableCompositeModification modification =
        new MutableCompositeModification();

    private final LoggingAdapter log =
        Logging.getLogger(getContext().system(), this);

    public ShardTransaction(DOMStoreReadWriteTransaction transaction,
        ActorRef shardActor) {
        this(null, transaction, shardActor);
    }

    public ShardTransaction(DOMStoreTransactionChain transactionChain, DOMStoreReadWriteTransaction transaction,
        ActorRef shardActor) {
        this.transactionChain = transactionChain;
        this.transaction = transaction;
        this.shardActor = shardActor;
    }



    public static Props props(final DOMStoreReadWriteTransaction transaction,
        final ActorRef shardActor) {
        return Props.create(new Creator<ShardTransaction>() {

            @Override
            public ShardTransaction create() throws Exception {
                return new ShardTransaction(transaction, shardActor);
            }
        });
    }

    public static Props props(final DOMStoreTransactionChain transactionChain, final DOMStoreReadWriteTransaction transaction,
        final ActorRef shardActor) {
        return Props.create(new Creator<ShardTransaction>() {

            @Override
            public ShardTransaction create() throws Exception {
                return new ShardTransaction(transactionChain, transaction, shardActor);
            }
        });
    }


    @Override
    public void handleReceive(Object message) throws Exception {
        if (message instanceof ReadData) {
            readData((ReadData) message);
        } else if (message instanceof WriteData) {
            writeData((WriteData) message);
        } else if (message instanceof MergeData) {
            mergeData((MergeData) message);
        } else if (message instanceof DeleteData) {
            deleteData((DeleteData) message);
        } else if (message instanceof ReadyTransaction) {
            readyTransaction((ReadyTransaction) message);
        } else if (message instanceof CloseTransaction) {
            closeTransaction((CloseTransaction) message);
        } else if (message instanceof GetCompositedModification) {
            // This is here for testing only
            getSender().tell(new GetCompositeModificationReply(
                new ImmutableCompositeModification(modification)), getSelf());
        }
    }

    private void readData(ReadData message) {
        final ActorRef sender = getSender();
        final ActorRef self = getSelf();
        final InstanceIdentifier path = message.getPath();
        final ListenableFuture<Optional<NormalizedNode<?, ?>>> future =
            transaction.read(path);

        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    Optional<NormalizedNode<?, ?>> optional = future.get();
                    if (optional.isPresent()) {
                        sender.tell(new ReadDataReply(optional.get()), self);
                    } else {
                        sender.tell(new ReadDataReply(null), self);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    log.error(e,
                        "An exception happened when reading data from path : "
                            + path.toString());
                }

            }
        }, getContext().dispatcher());
    }


    private void writeData(WriteData message) {
        modification.addModification(
            new WriteModification(message.getPath(), message.getData()));
        transaction.write(message.getPath(), message.getData());
        getSender().tell(new WriteDataReply(), getSelf());
    }

    private void mergeData(MergeData message) {
        modification.addModification(
            new MergeModification(message.getPath(), message.getData()));
        transaction.merge(message.getPath(), message.getData());
        getSender().tell(new MergeDataReply(), getSelf());
    }

    private void deleteData(DeleteData message) {
        modification.addModification(new DeleteModification(message.getPath()));
        transaction.delete(message.getPath());
        getSender().tell(new DeleteDataReply(), getSelf());
    }

    private void readyTransaction(ReadyTransaction message) {
        DOMStoreThreePhaseCommitCohort cohort = transaction.ready();
        ActorRef cohortActor = getContext().actorOf(
            ThreePhaseCommitCohort.props(cohort, shardActor, modification), "cohort");
        getSender()
            .tell(new ReadyTransactionReply(cohortActor.path()), getSelf());

    }

    private void closeTransaction(CloseTransaction message) {
        transaction.close();
        getSender().tell(new CloseTransactionReply(), getSelf());
        getSelf().tell(PoisonPill.getInstance(), getSelf());
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
