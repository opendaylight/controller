/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.broker.impl;

import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yangtools.restconf.client.api.RestconfClientContext;
import org.opendaylight.yangtools.yang.binding.RpcService;

public class RpcConsumerRegistryImpl implements RpcConsumerRegistry {

    private RestconfClientContext restconfClientContext;

    public RpcConsumerRegistryImpl(RestconfClientContext restconfClientContext){
        this.restconfClientContext = restconfClientContext;
    }
    @Override
    public <T extends RpcService> T getRpcService(Class<T> module) {
        return restconfClientContext.getRpcServiceContext(module).getRpcService();
    }
}
