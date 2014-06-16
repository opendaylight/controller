/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.Path;

/**
 * Transaction enabling client to have combined transaction,
 * which provides read and write capabilities.
 *
 * Initial state of write transaction is stable snapshot of current data tree
 * state captured when transaction was created and it's state and underlying
 * data tree are not affected by other concurrently running transactions.
 *
 * Write transaction is isolated from other concurrent write transactions in a
 * way, that this transaction does not see any state change introduced by other
 * concurrent transactions.
 *
 * @param <P> Type of path (subtree identifier), which represents location in tree
 * @param <D> Type of data (payload), which represents data payload
 */
public interface AsyncReadWriteTransaction<P extends Path<P>, D> extends AsyncReadTransaction<P, D>,
        AsyncWriteTransaction<P, D> {

}
