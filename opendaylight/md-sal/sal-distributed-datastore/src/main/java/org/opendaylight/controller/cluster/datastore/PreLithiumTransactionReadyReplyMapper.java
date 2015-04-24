/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import scala.concurrent.Future;
import akka.actor.ActorSelection;

/**
 * A {@link Mapper} extracting the {@link ActorSelection} pointing to the actor which
 * is backing a particular transaction. This class supports the Helium base release
 * behavior.
 */
@Deprecated
public final class PreLithiumTransactionReadyReplyMapper extends TransactionReadyReplyMapper {
    private final String transactionPath;

    private PreLithiumTransactionReadyReplyMapper(ActorContext actorContext, TransactionIdentifier identifier, final String transactionPath) {
        super(actorContext, identifier);
        this.transactionPath = Preconditions.checkNotNull(transactionPath);
    }

    @Override
    protected String extractCohortPathFrom(final ReadyTransactionReply readyTxReply) {
        return getActorContext().resolvePath(transactionPath, readyTxReply.getCohortPath());
    }

    public static Future<ActorSelection> transform(final Future<Object> readyReplyFuture, final ActorContext actorContext,
        final TransactionIdentifier identifier, final String transactionPath) {
        return readyReplyFuture.transform(new PreLithiumTransactionReadyReplyMapper(actorContext, identifier, transactionPath),
            SAME_FAILURE_TRANSFORMER, actorContext.getClientDispatcher());
    }
}
