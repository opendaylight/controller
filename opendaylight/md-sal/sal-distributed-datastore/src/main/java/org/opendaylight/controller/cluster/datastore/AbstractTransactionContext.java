/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.collect.Lists;
import java.util.List;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import scala.concurrent.Future;

abstract class AbstractTransactionContext implements TransactionContext {

    protected final TransactionIdentifier identifier;
    protected final List<Future<Object>> recordedOperationFutures = Lists.newArrayList();

    AbstractTransactionContext(TransactionIdentifier identifier) {
        this.identifier = identifier;
    }

    @Override
    public List<Future<Object>> getRecordedOperationFutures() {
        return recordedOperationFutures;
    }
}