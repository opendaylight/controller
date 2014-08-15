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
import org.opendaylight.controller.md.sal.dom.broker.impl.CachedDOMDataBrokerDecorator;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMDataBrokerImpl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class CachedForwardedBindingDataBrokerDecorator extends AbstractForwardedBindigDataBrokerDecorator{

    CachedDOMDataBrokerDecorator cachedDOMDataBroker;

    public CachedForwardedBindingDataBrokerDecorator(ForwardedBindingDataBroker bindingDataBroker, InstanceIdentifier<?> cachingPath) {
        super(bindingDataBroker);
        YangInstanceIdentifier cachingPathNormalized = getCodec().toNormalized(cachingPath);
        cachedDOMDataBroker = new CachedDOMDataBrokerDecorator((DOMDataBrokerImpl) bindingDataBroker.getDelegate(), cachingPathNormalized);
    }

    @Override
    public ReadWriteTransaction newReadWriteTransaction() {
        return new BindingDataReadWriteTransactionImpl(cachedDOMDataBroker.newReadWriteTransaction(),bindingDataBroker.getCodec());
    }

    @Override
    public WriteTransaction newWriteOnlyTransaction() {
        return new BindingDataWriteTransactionImpl<>(cachedDOMDataBroker.newWriteOnlyTransaction(),bindingDataBroker.getCodec());
    }
}
