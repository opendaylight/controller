/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package rpcbenchmark.impl;

import java.util.Set;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.RpcbenchPayloadService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class RoutedBindingRTCServer extends AbstractRpcbenchPayloadService implements AutoCloseable {
    private final Registration reg;

    RoutedBindingRTCServer(final RpcProviderService rpcProvider, final Set<InstanceIdentifier<?>> paths) {
        reg = rpcProvider.registerRpcImplementation(RpcbenchPayloadService.class, this, paths);
    }

    @Override
    public void close() {
        reg.close();
    }
}
