/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class CachedForwardedBindingDataBrokerDecorator extends AbstractForwardedBindigDataBrokerDecorator{

    InstanceIdentifier<?> cachingPath;
    NormalizedNodeCache cache;

    public CachedForwardedBindingDataBrokerDecorator(ForwardedBindingDataBroker bindingDataBroker, InstanceIdentifier<?> cachingPath) {
        super(bindingDataBroker);
        this.cachingPath = cachingPath;
        this.cache = new NormalizedNodeCache();
    }

    @Override
    public ReadWriteTransaction newReadWriteTransaction() {
        return new CachedBindingDataReadWriteTransaction(getDelegate().newReadWriteTransaction(), getCodec(), cachingPath, cache);
    }

    @Override
    public WriteTransaction newWriteOnlyTransaction() {
        return new CachedBindingDataWriteTransaction<>(getDelegate().newWriteOnlyTransaction(), getCodec(), cachingPath, cache);
    }
}
