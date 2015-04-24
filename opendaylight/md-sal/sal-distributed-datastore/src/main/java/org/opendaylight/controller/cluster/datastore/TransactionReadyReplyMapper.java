/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import akka.dispatch.Mapper;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * A {@link Mapper} extracting the {@link ActorSelection} pointing to the actor which
 * is backing a particular transaction.
 *
 * This class is not for general consumption. It is public only to support the pre-lithium compatibility
 * package.
 *
 * TODO: once we remove compatibility, make this class package-private and final.
 */
public class TransactionReadyReplyMapper extends Mapper<Object, ActorSelection> {
    protected static final Mapper<Throwable, Throwable> SAME_FAILURE_TRANSFORMER = new Mapper<Throwable, Throwable>() {
        @Override
        public Throwable apply(final Throwable failure) {
            return failure;
        }
    };
    private static final Logger LOG = LoggerFactory.getLogger(TransactionReadyReplyMapper.class);
    private final TransactionIdentifier identifier;
    private final ActorContext actorContext;

    protected TransactionReadyReplyMapper(final ActorContext actorContext, final TransactionIdentifier identifier) {
        this.actorContext = Preconditions.checkNotNull(actorContext);
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    protected final ActorContext getActorContext() {
        return actorContext;
    }

    protected String extractCohortPathFrom(final ReadyTransactionReply readyTxReply) {
        return readyTxReply.getCohortPath();
    }

    @Override
    public final ActorSelection checkedApply(final Object serializedReadyReply) {
        LOG.debug("Tx {} readyTransaction", identifier);

        // At this point the ready operation succeeded and we need to extract the cohort
        // actor path from the reply.
        if (ReadyTransactionReply.isSerializedType(serializedReadyReply)) {
            ReadyTransactionReply readyTxReply = ReadyTransactionReply.fromSerializable(serializedReadyReply);
            return actorContext.actorSelection(extractCohortPathFrom(readyTxReply));
        }

        // Throwing an exception here will fail the Future.
        throw new IllegalArgumentException(String.format("%s: Invalid reply type %s",
                identifier, serializedReadyReply.getClass()));
    }

    static Future<ActorSelection> transform(final Future<Object> readyReplyFuture, final ActorContext actorContext,
            final TransactionIdentifier identifier) {
        return readyReplyFuture.transform(new TransactionReadyReplyMapper(actorContext, identifier),
            SAME_FAILURE_TRANSFORMER, actorContext.getClientDispatcher());
    }
}
