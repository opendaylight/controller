/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.md.controller.topology.manager;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;

/**
 * Internal interface for submitted operations. Implementations of this
 * interface are enqueued and batched into data store transactions.
 */
interface TopologyOperation {
    /**
     * Execute the operation on top of the transaction.
     *
     * @param transaction Datastore transaction
     */
    void applyOperation(ReadWriteTransaction transaction);
}