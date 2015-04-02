/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

/**
 * Abstract superclass for transaction operations which should be executed
 * on a {@link TransactionContext} at a later point in time.
 */
abstract class TransactionOperation {
    /**
     * Execute the delayed operation.
     *
     * @param transactionContext
     */
    protected abstract void invoke(TransactionContext transactionContext);
}
