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
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import scala.concurrent.Future;

/**
 * A single component of a {@link TransactionProxy}. Provides data access
 * abstraction for a particular shard.
 */
abstract class AbstractTransactionComponent {

    private final TransactionIdentifier identifier;

    protected AbstractTransactionComponent(TransactionIdentifier identifier) {
        this.identifier = identifier;
    }

    TransactionIdentifier getIdentifier() {
        return identifier;
    }

    abstract CheckedFuture<Boolean, ReadFailedException> exists(YangInstanceIdentifier path);
    abstract CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(YangInstanceIdentifier path);

    abstract void delete(YangInstanceIdentifier path);
    abstract void merge(YangInstanceIdentifier path, NormalizedNode<?, ?> data);
    abstract void write(YangInstanceIdentifier path, NormalizedNode<?, ?> data);

    abstract void close();
    abstract Future<ActorSelection> coordinatedCommit();
    abstract AbstractThreePhaseCommitCohort<?> uncoordinatedCommit(ActorContext actorContext);
}
