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

/**
 * Write transaction provides mutation capabilities for a data tree.
 *
 * <p>
 * Initial state of write transaction is a stable snapshot of the current data tree.
 * The state is captured when the transaction is created and its state and underlying
 * data tree are not affected by other concurrently running transactions.
 * <p>
 * Write transactions are isolated from other concurrent write transactions. All
 * writes are local to the transaction and represent only a proposal of state
 * change for the data tree and it is not visible to any other concurrently running
 * transaction.
 * <p>
 * Applications publish the changes proposed in the transaction by calling {@link #commit}
 * on the transaction. This seals the transaction
 * (preventing any further writes using this transaction) and submits it to be
 * processed and applied to global conceptual data tree.
 * <p>
 * The transaction commit may fail due to a concurrent transaction modifying and committing data in
 * an incompatible way. See {@link #commit()} for more concrete commit failure examples.
 *
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
public interface AsyncWriteTransaction<P extends Path<P>, D> extends AsyncTransaction<P, D> {
    /**
     * Cancels the transaction.
     *
     * Transactions can only be cancelled if it's status is
     * {@link TransactionStatus#NEW} or {@link TransactionStatus#SUBMITED}
     *
     * Invoking cancel() on {@link TransactionStatus#FAILED} or
     * {@link TransactionStatus#CANCELED} will have no effect, and transaction
     * is considered cancelled.
     *
     * Invoking cancel() on finished transaction  (future returned by {@link #commit()}
     * already completed with {@link TransactionStatus#COMMITED}) will always
     * fail (return false).
     *
     * @return <tt>false</tt> if the task could not be cancelled,
     * typically because it has already completed normally;
     * <tt>true</tt> otherwise
     *
     */
    public boolean cancel();

    /**
     * Store a piece of data at specified path. This acts as an add / replace
     * operation, which is to say that whole subtree will be replaced by
     * specified path. Performing the following put operations:
     *
     * <pre>
     * 1) container { list [ a ] }
     * 2) container { list [ b ] }
     * </pre>
     *
     * will result in the following data being present:
     *
     * <pre>
     * container { list [ b ] }
     * </pre>
     *
     *
     * If you need to make sure that a parent object exists, but you do not want modify
     * its preexisting state by using put, consider using
     * {@link #merge(LogicalDatastoreType, Path, Object)}
     *
     * @param store
     *            Logical data store which should be modified
     * @param path
     *            Data object path
     * @param data
     *            Data object to be written to specified path
     * @throws IllegalStateException
     *             if the transaction is no longer {@link TransactionStatus#NEW}
     */
    public void put(LogicalDatastoreType store, P path, D data);

    /**
     * Store a piece of data at the specified path. This acts as a merge operation,
     * which is to say that any pre-existing data which is not explicitly
     * overwritten will be preserved. This means that if you store a container,
     * its child lists will be merged. Performing the following merge
     * operations:
     *
     * <pre>
     * 1) container { list [ a ] }
     * 2) container { list [ b ] }
     * </pre>
     *
     * will result in the following data being present:
     *
     * <pre>
     * container { list [ a, b ] }
     * </pre>
     *
     * This also means that storing the container will preserve any
     * augmentations which have been attached to it.
     *<p>
     * If you require an explicit replace operation, use
     * {@link #put(LogicalDatastoreType, Path, Object)} instead.
     *
     * @param store
     *            Logical data store which should be modified
     * @param path
     *            Data object path
     * @param data
     *            Data object to be written to specified path
     * @throws IllegalStateException
     *             if the transaction is no longer {@link TransactionStatus#NEW}
     */
    public void merge(LogicalDatastoreType store, P path, D data);

    /**
     * Remove a piece of data from specified path. This operation does not fail
     * if the specified path does not exist.
     *
     * @param store
     *            Logical data store which should be modified
     * @param path
     *            Data object path
     * @throws IllegalStateException
     *             if the transaction is no longer {@link TransactionStatus#NEW}
     */
    public void delete(LogicalDatastoreType store, P path);

    /**
     * Submits transaction to be applied to update logical data tree.
     * <p>
     * This call logically seals the transaction, which prevents the client from
     * further changing data tree using this transaction. Any subsequent calls to
     * {@link #put(LogicalDatastoreType, Path, Object)},
     * {@link #merge(LogicalDatastoreType, Path, Object)} or
     * {@link #delete(LogicalDatastoreType, Path)} will fail with
     * {@link IllegalStateException}.
     *
     * The transaction is marked as {@link TransactionStatus#SUBMITED} and
     * enqueued into the data store backed for processing.
     *
     * <p>
     * Whether or not the commit is successful is determined by versioning
     * of data tree and validation of registered commit participants
     * {@link AsyncConfigurationCommitHandler}
     * if transaction changes {@link LogicalDatastoreType#CONFIGURATION} data tree.
     *<p>
     * The effects of successful commit of data depends on
     * other data change listeners {@link AsyncDataChangeListener} and
     * {@link AsyncConfigurationCommitHandler}, which was registered to the
     * same {@link AsyncDataBroker}, to which this transaction belongs.
     *
     * <h2>Failure scenarios</h2>
     * <p>
     * Transaction may fail because of multiple reasons, such as
     * <ul>
     * <li>Another transaction finished earlier and modified the same node in
     * non-compatible way (see below). In this case the returned future will fail with
     * {@link OptimisticLockFailedException}. It is the responsibility of the
     * caller to create a new transaction and submit the same modification again in
     * order to update data tree.</li>
     * <li>Data change introduced by this transaction did not pass validation by
     * commit handlers or data was incorrectly structured. Returned future will
     * fail with {@link DataValidationFailedException}. User should not retry to
     * create new transaction with same data, since it probably will fail again.
     * </li>
     * </ul>
     *
     * <h3>Change compatibility</h3>
     *
     * There are several sets of changes which could be considered incompatible
     * between two transactions which are derived from same initial state.
     * Rules for conflict detection applies recursively for each subtree
     * level.
     *
     * <h4>Change compatibility of leafs, leaf-list items</h4>
     *
     * Following table shows  state changes and failures between two concurrent transactions,
     * which are based on same initial state, Tx 1 completes successfully
     * before Tx 2 is submitted.
     *
     * <table>
     * <tr><th>Initial state</th><th>Tx 1</th><th>Tx 2</th><th>Result</th></tr>
     * <tr><td>Empty</td><td>put(A,1)</td><td>put(A,2)</td><td>Tx 2 will fail, state is A=1</td></tr>
     * <tr><td>Empty</td><td>put(A,1)</td><td>merge(A,2)</td><td>A=2</td></tr>
     *
     * <tr><td>Empty</td><td>merge(A,1)</td><td>put(A,2)</td><td>Tx 2 will fail, state is A=1</td></tr>
     * <tr><td>Empty</td><td>merge(A,1)</td><td>merge(A,2)</td><td>A=2</td></tr>
     *
     *
     * <tr><td>A=0</td><td>put(A,1)</td><td>put(A,2)</td><td>Tx 2 will fail, A=1</td></tr>
     * <tr><td>A=0</td><td>put(A,1)</td><td>merge(A,2)</td><td>A=2</td></tr>
     * <tr><td>A=0</td><td>merge(A,1)</td><td>put(A,2)</td><td>Tx 2 will fail, A=1</td></tr>
     * <tr><td>A=0</td><td>merge(A,1)</td><td>merge(A,2)</td><td>A=2</td></tr>
     *
     * <tr><td>A=0</td><td>delete(A)</td><td>put(A,2)</td><td>Tx 2 will fail, A does not exists</td></tr>
     * <tr><td>A=0</td><td>delete(A)</td><td>merge(A,2)</td><td>A=2</td></tr>
     * </table>
     *
     * <h4>Change compatibility of subtrees</h4>
     *
     * Following table shows  state changes and failures between two concurrent transactions,
     * which are based on same initial state, Tx 1 completes successfully
     * before Tx 2 is submitted.
     *
     * <table>
     * <tr><th>Initial state</th><th>Tx 1</th><th>Tx 2</th><th>Result</th></tr>
     *
     * <tr><td>Empty</td><td>put(TOP,[])</td><td>put(TOP,[])</td><td>Tx 2 will fail, state is TOP=[]</td></tr>
     * <tr><td>Empty</td><td>put(TOP,[])</td><td>merge(TOP,[])</td><td>TOP=[]</td></tr>
     *
     * <tr><td>Empty</td><td>put(TOP,[FOO=1])</td><td>put(TOP,[BAR=1])</td><td>Tx 2 will fail, state is TOP=[FOO=1]</td></tr>
     * <tr><td>Empty</td><td>put(TOP,[FOO=1])</td><td>merge(TOP,[BAR=1])</td><td>TOP=[FOO=1,BAR=1]</td></tr>
     *
     * <tr><td>Empty</td><td>merge(TOP,[FOO=1])</td><td>put(TOP,[BAR=1])</td><td>Tx 2 will fail, state is TOP=[FOO=1]</td></tr>
     * <tr><td>Empty</td><td>merge(TOP,[FOO=1])</td><td>merge(TOP,[BAR=1])</td><td>TOP=[FOO=1,BAR=1]</td></tr>
     *
     * <tr><td>TOP=[]</td><td>put(TOP,[FOO=1])</td><td>put(TOP,[BAR=1])</td><td>Tx 2 will fail, state is TOP=[FOO=1]</td></tr>
     * <tr><td>TOP=[]</td><td>put(TOP,[FOO=1])</td><td>merge(TOP,[BAR=1])</td><td>state is TOP=[FOO=1,BAR=1]</td></tr>
     * <tr><td>TOP=[]</td><td>merge(TOP,[FOO=1])</td><td>put(TOP,[BAR=1])</td><td>Tx 2 will fail, state is TOP=[FOO=1]</td></tr>
     * <tr><td>TOP=[]</td><td>merge(TOP,[FOO=1])</td><td>merge(TOP,[BAR=1])</td><td>state is TOP=[FOO=1,BAR=1]</td></tr>
     * <tr><td>TOP=[]</td><td>delete(TOP)</td><td>put(TOP,[BAR=1])</td><td>Tx 2 will fail, state is empty store</td></tr>
     * <tr><td>TOP=[]</td><td>delete(TOP)</td><td>merge(TOP,[BAR=1])</td><td>state is TOP=[BAR=1]</td></tr>
     *
     * <tr><td>TOP=[]</td><td>put(TOP/FOO,1)</td><td>put(TOP/BAR,1])</td><td>state is TOP=[FOO=1,BAR=1]</td></tr>
     * <tr><td>TOP=[]</td><td>put(TOP/FOO,1)</td><td>merge(TOP/BAR,1)</td><td>state is TOP=[FOO=1,BAR=1]</td></tr>
     * <tr><td>TOP=[]</td><td>merge(TOP/FOO,1)</td><td>put(TOP/BAR,1)</td><td>state is TOP=[FOO=1,BAR=1]</td></tr>
     * <tr><td>TOP=[]</td><td>merge(TOP/FOO,1)</td><td>merge(TOP/BAR,1)</td><td>state is TOP=[FOO=1,BAR=1]</td></tr>
     * <tr><td>TOP=[]</td><td>delete(TOP)</td><td>put(TOP/BAR,1)</td><td>Tx 2 will fail, state is empty store</td></tr>
     * <tr><td>TOP=[]</td><td>delete(TOP)</td><td>merge(TOP/BAR,1]</td><td>Tx 2 will fail, state is empty store</td></tr>
     *
     * <tr><td>TOP=[FOO=1]</td><td>put(TOP/FOO,2)</td><td>put(TOP/BAR,1)</td><td>state is TOP=[FOO=2,BAR=1]</td></tr>
     * <tr><td>TOP=[FOO=1]</td><td>put(TOP/FOO,2)</td><td>merge(TOP/BAR,1)</td><td>state is TOP=[FOO=2,BAR=1]</td></tr>
     * <tr><td>TOP=[FOO=1]</td><td>merge(TOP/FOO,2)</td><td>put(TOP/BAR,1)</td><td>state is TOP=[FOO=2,BAR=1]</td></tr>
     * <tr><td>TOP=[FOO=1]</td><td>merge(TOP/FOO,2)</td><td>merge(TOP/BAR,1)</td><td>state is TOP=[FOO=2,BAR=1]</td></tr>
     * <tr><td>TOP=[FOO=1]</td><td>delete(TOP/FOO)</td><td>put(TOP/BAR,1)</td><td>state is TOP=[BAR=1]</td></tr>
     * <tr><td>TOP=[FOO=1]</td><td>delete(TOP/FOO)</td><td>merge(TOP/BAR,1]</td><td>state is TOP=[BAR=1]</td></tr>
     * </table>
     *
     *
     * <h3>Examples of failure scenarios</h3>
     *
     * <h4>Conflict of two transactions</h4>
     *
     * This example illustrates two concurrent transactions, which derived from
     * same initial state of data tree and proposes conflicting modifications.
     *
     * <pre>
     * txA = broker.newWriteTransaction(); // allocates new transaction, data tree is empty
     * txB = broker.newWriteTransaction(); // allocates new transaction, data tree is empty
     *
     * txA.put(CONFIGURATION, PATH, A);    // writes to PATH value A
     * txB.put(CONFIGURATION, PATH, B)     // writes to PATH value B
     *
     * ListenableFuture futureA = txA.commit(); // transaction A is sealed and committed
     * ListenebleFuture futureB = txB.commit(); // transaction B is sealed and committed
     * </pre>
     *
     * Commit of transaction A will be processed asynchronously and data tree
     * will be updated to contain value <code>A</code> for <code>PATH</code>.
     * Returned {@link ListenableFuture} will successfully complete once
     * state is applied to data tree.
     *
     * Commit of Transaction B will fail, because previous transaction also
     * modified path in a concurrent way. The state introduced by transaction B
     * will not be applied. Returned {@link ListenableFuture} object will fail
     * with {@link OptimisticLockFailedException} exception, which indicates to
     * client that concurrent transaction prevented the submitted transaction from being
     * applied.
     *
     * @return Result of the Commit, containing success information or list of
     *         encountered errors, if commit was not successful. The Future
     *         blocks until {@link TransactionStatus#COMMITED} is reached.
     *         Future will fail with {@link TransactionCommitFailedException} if
     *         Commit of this transaction failed. TODO: Usability: Consider
     *         change from ListenableFuture to
     *         {@link com.google.common.util.concurrent.CheckedFuture} which
     *         will throw {@link TransactionCommitFailedException}.
     *
     * @throws IllegalStateException
     *             if the transaction is not {@link TransactionStatus#NEW}
     */
    public ListenableFuture<RpcResult<TransactionStatus>> commit();

}
