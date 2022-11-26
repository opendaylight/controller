/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorSelection;
import akka.dispatch.Mapper;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * A {@link Mapper} extracting the {@link ActorSelection} pointing to the actor which
 * is backing a particular transaction.
 *
 * <p>
 * This class is not for general consumption. It is public only to support the pre-lithium compatibility
 * package.
 * TODO: once we remove compatibility, make this class package-private and final.
 */
public class TransactionReadyReplyMapper extends Mapper<Object, ActorSelection> {
    protected static final Mapper<Throwable, Throwable> SAME_FAILURE_TRANSFORMER = new Mapper<>() {
        @Override
        public Throwable apply(final Throwable failure) {
            return failure;
        }
    };
    private static final Logger LOG = LoggerFactory.getLogger(TransactionReadyReplyMapper.class);
    private final TransactionIdentifier identifier;
    private final ActorUtils actorUtils;

    protected TransactionReadyReplyMapper(final ActorUtils actorUtils, final TransactionIdentifier identifier) {
        this.actorUtils = requireNonNull(actorUtils);
        this.identifier = requireNonNull(identifier);
    }

    protected final ActorUtils getActorUtils() {
        return actorUtils;
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
            return actorUtils.actorSelection(extractCohortPathFrom(readyTxReply));
        }

        // Throwing an exception here will fail the Future.
        throw new IllegalArgumentException(String.format("%s: Invalid reply type %s",
                identifier, serializedReadyReply.getClass()));
    }

    static Future<ActorSelection> transform(final Future<Object> readyReplyFuture, final ActorUtils actorUtils,
            final TransactionIdentifier identifier) {
        return readyReplyFuture.transform(new TransactionReadyReplyMapper(actorUtils, identifier),
            SAME_FAILURE_TRANSFORMER, actorUtils.getClientDispatcher());
    }
}
