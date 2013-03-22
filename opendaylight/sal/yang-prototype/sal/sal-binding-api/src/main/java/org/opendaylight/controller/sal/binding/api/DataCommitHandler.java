/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import java.util.Set;

import org.opendaylight.controller.sal.binding.api.BindingAwareProvider.ProviderFunctionality;
import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.controller.yang.common.RpcResult;


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
 * <li>For each <code>CommitTransaction</code> from Commit Request phase
 * <ol>
 * <li><code>Broker</code>
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * broker invokes a {@link CommitTransaction#finish()}
 * <li>The provider rollbacks a commit and returns an {@link RpcResult} of
 * rollback. </ol>
 * <li>Broker returns a error result to the consumer.
 * 
 * 
 * <h3>Registration of functionality</h3>
 * The registration could be done by :
 * <ul>
 * <li>returning an instance of implementation in the return value of
 * {@link Provider#getProviderFunctionality()}
 * <li>passing an instance of implementation and {@link DataStoreIdentifier} of
 * rpc as arguments to the
 * {@link DataProviderService#addCommitHandler(DataStoreIdentifier, DataCommitHandler)}
 * </ul>
 * 

 * 
 */
public interface DataCommitHandler extends ProviderFunctionality {
    /**
     * A set of Data Stores supported by implementation.
     * 
     * The set of {@link DataStoreIdentifier}s which identifies target data
     * stores which are supported by this commit handler. This set is used, when
     * {@link Provider} is registered to the SAL, to register and expose the
     * commit handler functionality to affected data stores.
     * 
     * @return Set of Data Store identifiers
     */
    Set<DataStoreIdentifier> getSupportedDataStores();

    /**
     * The provider (commit handler) starts a commit transaction.
     * 
     * <p>
     * The commit handler (provider) prepares an commit scenario, rollback
     * scenario and validates data.
     * 
     * <p>
     * If the provider is aware that at this point the commit would not be
     * successful, the transaction is not created, but list of errors which
     * prevented the start of transaction are returned.
     * 
     * @param store
     * @return Transaction object representing this commit, errors otherwise.
     */
    RpcResult<CommitTransaction> requestCommit(DataStoreIdentifier store);

    public interface CommitTransaction {
        /**
         * 
         * @return Data store affected by the transaction
         */
        DataStoreIdentifier getDataStore();

        /**
         * Returns the handler associated with this transaction.
         * 
         * @return Handler
         */
        DataCommitHandler getHandler();

        /**
         * 
         * Finishes a commit.
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
         * @return
         * @throws IllegalStateException
         *             If the method is invoked after {@link #finish()}
         */
        RpcResult<Void> rollback() throws IllegalStateException;
    }

}
