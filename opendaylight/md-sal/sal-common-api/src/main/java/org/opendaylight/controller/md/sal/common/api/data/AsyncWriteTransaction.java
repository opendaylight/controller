/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncWriteTransaction<P extends Path<P>, D>  extends AsyncTransaction<P, D> {
    /**
     * Cancels transaction.
     *
     * Transaction could be only cancelled if it's status
     * is {@link TransactionStatus#NEW} or {@link TransactionStatus#SUBMITED}
     *
     * Invoking cancel() on {@link TransactionStatus#FAILED} or {@link TransactionStatus#CANCELED}
     * will have no effect.
     *
     * @throws IllegalStateException If transaction status is {@link TransactionStatus#COMMITED}
     *
     */
    public void cancel();

    /**
     * Store a piece of data at specified path. This acts as a add / replace operation,
     * which is to say that whole subtree will be replaced by specified path.
     *
     * If you need add or merge of current object with specified use {@link #merge(LogicalDatastoreType, Path, Object)}
     *
     * @param store Logical data store which should be modified
     * @param path Data object path
     * @param data Data object to be written to specified path
     * @throws IllegalStateException if the transaction is no longer {@link TransactionStatus#NEW}
     */
    public void put(LogicalDatastoreType store, P path, D data);

    /**
     * Store a piece of data at specified path. This acts as a merge operation,
     * which is to say that any pre-existing data which is not explicitly
     * overwritten will be preserved. This means that if you store a container,
     * its child lists will be merged. Performing the following put operations:
     *
     * 1) container { list [ a ] }
     * 2) container { list [ b ] }
     *
     * will result in the following data being present:
     *
     * container { list [ a, b ] }
     *
     * This also means that storing the container will preserve any augmentations
     * which have been attached to it.
     *
     * If you require an explicit replace operation, use {@link #put(LogicalDatastoreType, Path, Object)} instead.
     *
     * @param store Logical data store which should be modified
     * @param path Data object path
     * @param data Data object to be written to specified path
     * @throws IllegalStateException if the transaction is no longer {@link TransactionStatus#NEW}
     */
    public void merge(LogicalDatastoreType store, P path, D data);

    /**
     * Remove a piece of data from specified path. This operation does not fail
     * if the specified path does not exist.
     *
     * @param store Logical data store which should be modified
     * @param path Data object path
     * @throws IllegalStateException if the transaction is no longer {@link TransactionStatus#NEW}
     */
    public void delete(LogicalDatastoreType store, P path);

    /**
     *
     * Closes transaction and resources allocated to the transaction.
     *
     * This call does not change Transaction status. Client SHOULD
     * explicitly {@link #commit()} or {@link #cancel()} transaction.
     *
     * @throws IllegalStateException if the transaction has not been
     *         updated by invoking {@link #commit()} or {@link #cancel()}.
     */
    @Override
    public void close();

    /**
     * Initiates a commit of modification. This call logically seals the
     * transaction, preventing any the client from interacting with the
     * data stores. The transaction is marked as {@link TransactionStatus#SUBMITED}
     * and enqueued into the data store backed for processing.
     *
     * <p>
     * The successful commit changes the state of the system and may affect
     * several components.
     *
     * <p>
     * The effects of successful commit of data are described in the
     * specifications and YANG models describing the Provider components of
     * controller. It is assumed that Consumer has an understanding of this
     * changes.
     *
     * @see DataCommitHandler for further information how two-phase commit is
     *      processed.
     * @param store Identifier of the store, where commit should occur.
     * @return Result of the Commit, containing success information or list of
     *         encountered errors, if commit was not successful. The Future
     *         blocks until {@link TransactionStatus#COMMITED} or
     *         {@link TransactionStatus#FAILED} is reached.
     * @throws IllegalStateException if the transaction is not {@link TransactionStatus#NEW}
     */
    public ListenableFuture<RpcResult<TransactionStatus>> commit();

}
