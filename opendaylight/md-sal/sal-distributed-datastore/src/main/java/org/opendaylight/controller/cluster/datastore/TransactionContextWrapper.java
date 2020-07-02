/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import akka.actor.ActorSelection;
import akka.dispatch.Futures;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * A helper class that wraps an eventual TransactionContext instance. Operations destined for the target
 * TransactionContext instance are cached until the TransactionContext instance becomes available at which
 * time they are executed.
 *
 * @author Thomas Pantelis
 */
abstract class TransactionContextWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionContextWrapper.class);

    abstract TransactionContext getTransactionContext();

    abstract TransactionIdentifier getIdentifier();

    abstract OperationLimiter getLimiter();

    abstract void maybeExecuteTransactionOperation(TransactionOperation op);

    abstract void executePriorTransactionOperations(TransactionContext localTransactionContext);

    abstract Future<ActorSelection> readyTransaction(Optional<SortedSet<String>> participatingShardNames);
}
