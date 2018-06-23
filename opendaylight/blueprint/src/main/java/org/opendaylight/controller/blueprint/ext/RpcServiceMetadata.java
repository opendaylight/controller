/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import java.util.function.Predicate;
import org.opendaylight.mdsal.dom.spi.RpcRoutingStrategy;

/**
 * Factory metadata corresponding to the "rpc-service" element that gets an RPC service implementation from
 * the RpcProviderRegistry and provides it to the Blueprint container.
 *
 * @author Thomas Pantelis
 */
final class RpcServiceMetadata extends AbstractInvokableServiceMetadata {
    RpcServiceMetadata(final String id, final String interfaceName) {
        super(id, interfaceName);
    }

    @Override
    Predicate<RpcRoutingStrategy> rpcFilter() {
        return s -> !s.isContextBasedRouted();
    }
}
