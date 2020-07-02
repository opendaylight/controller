/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import java.util.Optional;
import java.util.SortedSet;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * A helper class that wraps an eventual TransactionContext instance. Operations destined for the target
 * TransactionContext instance are cached until the TransactionContext instance becomes available at which
 * time they are executed.
 *
 * @author Thomas Pantelis
 */
abstract class TransactionContextWrapper {

    abstract TransactionContext getTransactionContext();

    abstract TransactionIdentifier getIdentifier();

    abstract OperationLimiter getLimiter();

    abstract void maybeExecuteTransactionOperation(TransactionOperation op);

    abstract void executePriorTransactionOperations(TransactionContext localTransactionContext);

    abstract Future<ActorSelection> readyTransaction(Optional<SortedSet<String>> participatingShardNames);
}
