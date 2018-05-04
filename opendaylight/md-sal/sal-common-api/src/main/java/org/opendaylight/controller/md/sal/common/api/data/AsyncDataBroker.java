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
 * Base interface that provides access to a conceptual data tree store and also provides the ability to
 * subscribe for changes to data under a given branch of the tree.
 *
 * <p>
 * All operations on the data tree are performed via one of the transactions:
 * <ul>
 * <li>Read-Only - allocated using {@link #newReadOnlyTransaction()}
 * <li>Write-Only - allocated using {@link #newWriteOnlyTransaction()}
 * <li>Read-Write - allocated using {@link #newReadWriteTransaction()}
 * </ul>
 *
 * <p>
 * These transactions provide a stable isolated view of data tree, which is
 * guaranteed to be not affected by other concurrent transactions, until
 * transaction is committed.
 *
 * <p>
 * For a detailed explanation of how transaction are isolated and how transaction-local
 * changes are committed to global data tree, see
 * {@link AsyncReadTransaction}, {@link AsyncWriteTransaction},
 * {@link AsyncReadWriteTransaction} and {@link AsyncWriteTransaction#submit()}.
 *
 *
 * <p>
 * It is strongly recommended to use the type of transaction, which
 * provides only the minimal capabilities you need. This allows for
 * optimizations at the data broker / data store level. For example,
 * implementations may optimize the transaction for reading if they know ahead
 * of time that you only need to read data - such as not keeping additional meta-data,
 * which may be required for write transactions.
 *
 * <p>
 * <b>Implementation Note:</b> This interface is not intended to be implemented
 * by users of MD-SAL, but only to be consumed by them.
 *
 * @param <P>
 *            Type of path (subtree identifier), which represents location in
 *            tree
 * @param <D>
 *            Type of data (payload), which represents data payload
 */
public interface AsyncDataBroker<P extends Path<P>, D> extends AsyncDataTransactionFactory<P, D> {

    @Override
    AsyncReadOnlyTransaction<P, D> newReadOnlyTransaction();

    @Override
    AsyncReadWriteTransaction<P, D> newReadWriteTransaction();

    @Override
    AsyncWriteTransaction<P, D> newWriteOnlyTransaction();
}
