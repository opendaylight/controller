/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.List;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import scala.concurrent.Future;

/*
 * FIXME: why do we need this interface? It should be possible to integrate it with
 *        AbstractTransactionContext, which is the only implementation anyway.
 */
interface TransactionContext {
    void closeTransaction();

    Future<ActorSelection> readyTransaction();

    void writeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data);

    void deleteData(YangInstanceIdentifier path);

    void mergeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data);

    CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readData(
            final YangInstanceIdentifier path);

    CheckedFuture<Boolean, ReadFailedException> dataExists(YangInstanceIdentifier path);

    List<Future<Object>> getRecordedOperationFutures();
}