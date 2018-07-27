/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.api;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainFactory;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Provides access to a conceptual data tree store and also provides the ability to
 * subscribe for changes to data under a given branch of the tree.
 *
 * <p>
 * For more information on usage, please see the documentation in {@link AsyncDataBroker}.
 *
 * @see AsyncDataBroker
 * @see TransactionChainFactory
 *
 * @deprecated Use {@link org.opendaylight.mdsal.binding.api.DataBroker} instead
 */
@Deprecated
public interface DataBroker extends  AsyncDataBroker<InstanceIdentifier<?>, DataObject>,
        TransactionChainFactory<InstanceIdentifier<?>, DataObject>, TransactionFactory, BindingService,
        DataTreeChangeService {
    @Override
    ReadOnlyTransaction newReadOnlyTransaction();

    @Override
    ReadWriteTransaction newReadWriteTransaction();

    @Override
    WriteTransaction newWriteOnlyTransaction();

    @Override
    BindingTransactionChain createTransactionChain(TransactionChainListener listener);
}
