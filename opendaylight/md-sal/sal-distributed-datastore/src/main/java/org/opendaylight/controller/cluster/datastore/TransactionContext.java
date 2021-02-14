/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Optional;
import java.util.SortedSet;
import org.opendaylight.controller.cluster.datastore.messages.AbstractRead;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import scala.concurrent.Future;

/*
 * FIXME: why do we need this interface? It should be possible to integrate it with
 *        AbstractTransactionContext, which is the only implementation anyway.
 */
interface TransactionContext {
    void closeTransaction();

    Future<ActorSelection> readyTransaction(Boolean havePermit, Optional<SortedSet<String>> participatingShardNames);

    <T> void executeRead(AbstractRead<T> readCmd, SettableFuture<T> promise, Boolean havePermit);

    void executeDelete(YangInstanceIdentifier path, Boolean havePermit);

    void executeMerge(YangInstanceIdentifier path, NormalizedNode data, Boolean havePermit);

    void executeWrite(YangInstanceIdentifier path, NormalizedNode data, Boolean havePermit);

    Future<Object> directCommit(Boolean havePermit);

    /**
     * Invoked by {@link TransactionContextWrapper} when it has finished handing
     * off operations to this context. From this point on, the context is responsible
     * for throttling operations.
     *
     * <p>
     * Implementations can rely on the wrapper calling this operation in a synchronized
     * block, so they do not need to ensure visibility of this state transition themselves.
     */
    void operationHandOffComplete();

    /**
     * A TransactionContext that uses operation limiting should return true else false.
     *
     * @return true if operation limiting is used, false otherwise
     */
    boolean usesOperationLimiting();

    short getTransactionVersion();
}
