/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import scala.concurrent.Future;

abstract class AbstractTransactionContext implements TransactionContext {

    private final List<Future<Object>> recordedOperationFutures = new ArrayList<>();
    private final TransactionIdentifier identifier;

    protected AbstractTransactionContext(TransactionIdentifier identifier) {
        this.identifier = identifier;
    }

    @Override
    public final void copyRecordedOperationFutures(Collection<Future<Object>> target) {
        target.addAll(recordedOperationFutures);
    }

    protected final TransactionIdentifier getIdentifier() {
        return identifier;
    }

    protected final Collection<Future<Object>> copyRecordedOperationFutures() {
        return ImmutableList.copyOf(recordedOperationFutures);
    }

    protected final int recordedOperationCount() {
        return recordedOperationFutures.size();
    }

    protected final void recordOperationFuture(Future<Object> future) {
        recordedOperationFutures.add(future);
    }
}
