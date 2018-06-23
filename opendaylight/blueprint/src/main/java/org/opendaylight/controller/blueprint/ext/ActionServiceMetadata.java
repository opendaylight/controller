/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import java.util.function.Predicate;
import org.opendaylight.mdsal.dom.spi.RpcRoutingStrategy;

/**
 * Factory metadata corresponding to the "action-service" element. It waits for a DOM promise of registration
 * to appear in the {@link DOMRpcService} and then acquires a dynamic proxy via RpcProviderRegistry.
 *
 * @author Robert Varga
 */
final class ActionServiceMetadata extends AbstractInvokableServiceMetadata {
    /*
     * Implementation note:
     *
     * This implementation assumes Binding V1 semantics for actions, which means actions are packaged along with RPCs
     * into a single interface. This has interesting implications on working with RpcServiceMetadata, which only
     * handles the RPC side of the contract.
     *
     * Further interesting interactions stem from the fact that in DOM world each action is a separate entity, so the
     * interface contract can let some actions to be invoked, while failing for others. This is a shortcoming of the
     * Binding Specification and will be addressed in Binding V2 -- where each action is its own interface.
     */
    ActionServiceMetadata(final String id, final String interfaceName) {
        super(id, interfaceName);
    }

    @Override
    Predicate<RpcRoutingStrategy> rpcFilter() {
        return RpcRoutingStrategy::isContextBasedRouted;
    }
}
