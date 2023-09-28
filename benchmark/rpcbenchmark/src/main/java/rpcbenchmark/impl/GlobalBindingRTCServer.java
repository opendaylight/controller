/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package rpcbenchmark.impl;

import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GlobalBindingRTCServer extends AbstractRpcbenchPayloadService implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalBindingRTCServer.class);

    private final Registration reg;

    GlobalBindingRTCServer(@Reference final RpcProviderService rpcProvider) {
        reg = rpcProvider.registerRpcImplementations(getRpcClassToInstanceMap());
        LOG.debug("GlobalBindingRTCServer started");
    }

    @Override
    public void close() {
        reg.close();
        LOG.debug("GlobalBindingRTCServer stopped");
    }
}
