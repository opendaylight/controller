/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.api;

import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * A chain of transactions. Transactions in a chain need to be committed in
 * sequence and each transaction should see the effects of previous transactions
 * as if they happened. A chain makes no guarantees of atomicity, in fact
 * transactions are committed as soon as possible, but in order as they were
 * allocated.
 * <p>
 * For more information about transaction chaining and transaction chains
 * see {@link TransactionChain}.
 *
 * @see TransactionChain
 *
 */
public interface BindingTransactionChain extends TransactionFactory, TransactionChain<InstanceIdentifier<?>, DataObject> {
    /**
     * {@inheritDoc}
     */
    @Override
    ReadOnlyTransaction newReadOnlyTransaction();

    /**
     * {@inheritDoc}
     */
    @Override
    ReadWriteTransaction newReadWriteTransaction();

    /**
     * {@inheritDoc}
     */
    @Override
    WriteTransaction newWriteOnlyTransaction();
}
