/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import org.opendaylight.yangtools.yang.binding.RpcService;

/**
 * Base interface defining contract for retrieving MD-SAL
 * version of RpcServices
 * 
 */
public interface RpcConsumerRegistry extends BindingAwareService {
    /**
     * Returns a session specific instance (implementation) of requested
     * YANG module implentation / service provided by consumer.
     * 
     * @return Session specific implementation of service
     */
    <T extends RpcService> T getRpcService(Class<T> module);
}
