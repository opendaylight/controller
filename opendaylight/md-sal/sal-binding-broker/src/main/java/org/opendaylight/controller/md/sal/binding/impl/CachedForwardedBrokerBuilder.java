/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMDataBrokerImpl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class CachedForwardedBrokerBuilder {
    public static DataBroker create(DataBroker dataBroker, InstanceIdentifier<?> instanceIdentifier) {
        if (dataBroker instanceof ForwardedBindingDataBroker &&
                ((ForwardedBindingDataBroker) dataBroker).getDelegate() instanceof DOMDataBrokerImpl) {
            return new CachedForwardedBindingDataBrokerDecorator((ForwardedBindingDataBroker) dataBroker, instanceIdentifier);
        }
        else {
            //TODO: throw exception when cached data broker cannot be created form provided broker
            return null;
        }
    }
}
