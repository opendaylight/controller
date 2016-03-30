/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.serialization.Serialization;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardTransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ShardLeaderBehavior extends ShardBehavior {
    private static final Logger LOG = LoggerFactory.getLogger(ShardLeaderBehavior.class);
    private final RaftState state;

    ShardLeaderBehavior(final Shard shard, final RaftState state) {
        super(shard);
        this.state = Preconditions.checkNotNull(state);
    }

    @Override
    ShardCandidateBehavior becomeCandidate() {
        return new ShardCandidateBehavior(getShard());
    }

    @Override
    ShardFollowerBehavior becomeFollower() {
        return new ShardFollowerBehavior(getShard());
    }

    private ShardLeaderBehavior nextLeader(final RaftState newState) {
        return state == newState ? this : new ShardLeaderBehavior(getShard(), newState);
    }

    @Override
    ShardLeaderBehavior becomeIsolatedLeader() {
        return nextLeader(RaftState.IsolatedLeader);
    }

    @Override
    ShardLeaderBehavior becomeLeader() {
        return nextLeader(RaftState.Leader);
    }


    @Override
    void handleCommitTransaction(final ActorRef sender, final CommitTransaction message) {
        getShard().commitTransaction(message.getTransactionID(), sender);
    }

    @Override
    void handleCreateTransaction(final ActorRef sender, final Object message) {
        createTransaction(sender, CreateTransaction.fromSerializable(message));
    }

    private boolean failIfIsolatedLeader(final ActorRef sender) {
        if (isIsolatedLeader()) {
            sender.tell(new akka.actor.Status.Failure(new NoShardLeaderException(String.format(
                    "Shard %s was the leader but has lost contact with all of its followers. Either all" +
                    " other follower nodes are down or this node is isolated by a network partition.",
                    persistenceId()))), getSelf());
            return true;
        }

        return false;
    }

    private boolean isIsolatedLeader() {
        return state == RaftState.IsolatedLeader;
    }

    private void createTransaction(final ActorRef sender, final CreateTransaction createTransaction) {
        try {
            final TransactionType type = TransactionType.fromInt(createTransaction.getTransactionType());
            if (type != TransactionType.READ_ONLY && failIfIsolatedLeader(sender)) {
                return;
            }

            ActorRef transactionActor = createTransaction(type, createTransaction.getTransactionId(),
                createTransaction.getTransactionChainId());

            sender.tell(new CreateTransactionReply(Serialization.serializedActorPath(transactionActor),
                    createTransaction.getTransactionId(), createTransaction.getVersion()).toSerializable(), getSelf());
        } catch (Exception e) {
            sender.tell(new akka.actor.Status.Failure(e), getSelf());
        }
    }

    private ActorRef createTransaction(TransactionType type, String remoteTransactionId, String transactionChainId) {
        ShardTransactionIdentifier transactionId = new ShardTransactionIdentifier(remoteTransactionId);

        LOG.debug("{}: Creating transaction : {} ", persistenceId(), transactionId);

        return getShard().createTypedTransactionActor(type, transactionId, transactionChainId);
    }
}
