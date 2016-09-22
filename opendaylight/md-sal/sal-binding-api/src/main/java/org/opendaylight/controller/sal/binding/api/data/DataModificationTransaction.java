/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api.data;

import java.util.EventListener;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 *
 *
 * @deprecated Replaced by more specific transaction types. Please use
 *          {@link org.opendaylight.controller.md.sal.binding.api.DataBroker#newReadOnlyTransaction(),
 *          {@link org.opendaylight.controller.md.sal.binding.api.DataBroker#newReadWriteTransaction()
 *          or
 *          {@link org.opendaylight.controller.md.sal.binding.api.DataBroker#newWriteOnlyTransaction().
 *
 *
 */
@Deprecated
public interface DataModificationTransaction extends
        DataModification<InstanceIdentifier<? extends DataObject>, DataObject> {
    /**
     * Returns an unique identifier for transaction
     *
     */
    @Override
    Object getIdentifier();

    /**
     * Initiates a two-phase commit of candidate data.
     *
     * <p>
     * The {@link Consumer} could initiate a commit of candidate data
     *
     * <p>
     * The successful commit changes the state of the system and may affect
     * several components.
     *
     * <p>
     * The effects of successful commit of data are described in the
     * specifications and YANG models describing the {@link Provider} components
     * of controller. It is assumed that {@link Consumer} has an understanding
     * of this changes.
     *
     *
     * @see org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler for further information how two-phase commit is
     *      processed.
     * @param store
     *            Identifier of the store, where commit should occur.
     * @return Result of the commit, containing success information or list of
     *         encountered errors, if commit was not successful.
     */
    @Override
    Future<RpcResult<TransactionStatus>> commit();

    /**
     * Register a listener for transaction
     *
     * @param listener
     * @return
     */
    ListenerRegistration<DataTransactionListener> registerListener(DataTransactionListener listener);

    /**
     * Listener for transaction state changes
     */
    interface DataTransactionListener extends EventListener {
        /**
         * Callback is invoked after each transaction status change.
         *
         * @param transaction Transaction
         * @param status New status
         */
        void onStatusUpdated(DataModificationTransaction transaction,TransactionStatus status);
    }
}
