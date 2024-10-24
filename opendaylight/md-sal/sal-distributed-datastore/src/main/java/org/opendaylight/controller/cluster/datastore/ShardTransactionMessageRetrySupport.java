/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Cancellable;
import org.apache.pekko.actor.Status.Failure;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * Supporting class for Shard that maintains state for retrying transaction messages when there is no leader.
 *
 * @author Thomas Pantelis
 */
@Deprecated(since = "9.0.0", forRemoval = true)
class ShardTransactionMessageRetrySupport implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(ShardTransactionMessageRetrySupport.class);

    static final Class<?> TIMER_MESSAGE_CLASS = MessageInfo.class;

    private final Set<MessageInfo> messagesToRetry = new LinkedHashSet<>();
    private final Shard shard;

    ShardTransactionMessageRetrySupport(final Shard shard) {
        this.shard = shard;
    }

    void addMessageToRetry(final Object message, final ActorRef replyTo, final String failureMessage) {
        LOG.debug("{}: Adding message {} to retry", shard.persistenceId(), message);

        MessageInfo messageInfo = new MessageInfo(message, replyTo, failureMessage);

        FiniteDuration period = shard.getDatastoreContext().getShardRaftConfig().getElectionTimeOutInterval().$times(2);
        messageInfo.timer = shard.getContext().system().scheduler().scheduleOnce(period, shard.getSelf(),
                messageInfo, shard.getContext().dispatcher(), ActorRef.noSender());

        messagesToRetry.add(messageInfo);
    }

    void retryMessages() {
        if (messagesToRetry.isEmpty()) {
            return;
        }

        MessageInfo[] copy = messagesToRetry.toArray(new MessageInfo[messagesToRetry.size()]);
        messagesToRetry.clear();

        for (MessageInfo info: copy) {
            LOG.debug("{}: Retrying message {}", shard.persistenceId(), info.message);
            info.retry(shard);
        }
    }

    void onTimerMessage(final Object message) {
        MessageInfo messageInfo = (MessageInfo)message;

        LOG.debug("{}: Timer expired for message {}", shard.persistenceId(), messageInfo.message);

        messagesToRetry.remove(messageInfo);
        messageInfo.timedOut(shard);
    }

    @Override
    public void close() {
        for (MessageInfo info: messagesToRetry) {
            info.timedOut(shard);
        }

        messagesToRetry.clear();
    }

    private static final class MessageInfo {
        final Object message;
        final ActorRef replyTo;
        final String failureMessage;
        Cancellable timer;

        MessageInfo(final Object message, final ActorRef replyTo, final String failureMessage) {
            this.message = message;
            this.replyTo = replyTo;
            this.failureMessage = requireNonNull(failureMessage);
        }

        void retry(final Shard shard) {
            timer.cancel();
            shard.getSelf().tell(message, replyTo);
        }

        void timedOut(final Shard shard) {
            replyTo.tell(new Failure(new NoShardLeaderException(failureMessage, shard.persistenceId())),
                    shard.getSelf());
        }
    }
}
