/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * A chain of DOM Data transactions.
 *
 * Transactions in a chain need to be committed in sequence and each
 * transaction should see the effects of previous transactions as if they happened. A chain
 * makes no guarantees of atomicity, in fact transactions are committed as soon as possible.
 *
 * <p>
 * This interface is type capture of {@link TransactionChain} for DOM Data Contracts.
 */
public interface DOMTransactionChain extends TransactionChain<YangInstanceIdentifier, NormalizedNode<?, ?>> {

    @Override
    DOMDataReadOnlyTransaction newReadOnlyTransaction();

    @Override
    DOMDataReadWriteTransaction newReadWriteTransaction();

    @Override
    DOMDataWriteTransaction newWriteOnlyTransaction();

}
