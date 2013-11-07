/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.controller.sal.common.DataStoreIdentifier;
// FIXME: After 0.6 Release of YANGTools refactor to use Path marker interface for arguments.
// import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.yang.common.RpcResult;
/**
 * Two phase commit handler (cohort) of the two-phase commit protocol of data.
 * 
 * <p>
 * The provider should expose the implementation of DataCommitHandler if it's
 * functionality depends on any subset of data stored in data repositories, in
 * order to participate in {@link DataBrokerService#commit(DataStoreIdentifier)
 * operation.
 * 
 * <p>
 * Operations of two-phase commit handlers should not change data in data store,
 * this is responsibility of the coordinator (broker or some component of the
 * broker).
 * 
 * The commit handlers are responsible for changing the internal state of the
 * provider to reflect the commited changes in data.
 * 
 * <h3>Two-phase commit</h3>
 * 
 * <h4>Commit Request Phase</h4>
 * 
 * <ol>
 * <li> <code>Consumer</code> edits data by invocation of
 * <code>DataBrokerService.editCandidateData(DataStoreIdentifier, DataRoot)</code>
 * <li> <code>Consumer</code> starts a commit by invoking
 * <code>DataBrokerService.commit(DataStoreIdentifier)</code>
 * <li> <code>Broker</code> retrieves a list of all registered
 * <code>DataCommitHandlers</code>
 * <li>For each <code>DataCommitHandler</code>
 * <ol>
 * <li><code>Broker</code> invokes a
 * <code>DataCommitHandler.requestCommit(DataStoreIdentifier)</code> operation.
 * <li><code>DataCommitHandler</code> returns a <code>RpcResult</code> with
 * <code>CommitTransaction</code>
 * <li>If the result was successful, broker adds <code>CommitTransaction</code>
 * to the list of opened transactions. If not, brokers stops a commit request
 * phase and starts a rollback phase.
 * </ol>
 * <li><code>Broker</code> starts a commit finish phase
 * </ol>
 * 
 * <h4>Commit Finish Phase</h4>
 * 
 * <ol>
 * <li>For each <code>CommitTransaction</code> from Commit Request phase
 * <ol>
 * <li><code>Broker</code> broker invokes a
 * <code>CommitTransaction.finish()</code>
 * <li>The provider finishes a commit (applies the change) and returns an
 * <code>RpcResult</code>.
 * </ol>
 * <li>
 * <ul>
 * <li>If all returned results means successful, the brokers end two-phase
 * commit by returning a success commit result to the Consumer.
 * <li>If error occured, the broker starts a commit rollback phase.
 * </ul>
 * </ol>
 * 
 * <h4>Commit Rollback Phase</h4>
 * <li>For each <code>DataCommitTransaction</code> from Commit Request phase
 * <ol>
 * <li><code>Broker</code>
 * broker invokes a {@link DataCommitTransaction#finish()}
 * <li>The provider rollbacks a commit and returns an {@link RpcResult} of
 * rollback. </ol>
 * <li>Broker returns a error result to the consumer.
 * 
 * @param <P> Class representing a path
 * @param <D> Superclass from which all data objects are derived from.
 */
public interface DataCommitHandler<P/* extends Path<P> */,D> {

    
    DataCommitTransaction<P, D> requestCommit(DataModification<P,D> modification);

    public interface DataCommitTransaction<P/* extends Path<P> */,D> {

        DataModification<P,D> getModification();

        /**
         * 
         * Finishes a commit.
         * 
         * This callback is invoked by commit coordinator to finish commit action.
         * 
         * The implementation is required to finish transaction or return unsuccessful
         * rpc result if something went wrong.
         * 
         * The provider (commit handler) should apply all changes to its state
         * which are a result of data change-
         * 
         * @return
         */
        RpcResult<Void> finish() throws IllegalStateException;

        /**
         * Rollbacks a commit.
         * 
         * This callback is invoked by commit coordinator to finish commit action.
         * 
         * The provider (commit handler) should rollback all changes to its state
         * which were a result of previous request commit.
         * 
         * @return
         * @throws IllegalStateException
         *             If the method is invoked after {@link #finish()}
         */
        RpcResult<Void> rollback() throws IllegalStateException;
    }

}
