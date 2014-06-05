/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opendaylight.yangtools.yang.binding.BaseIdentity;

import com.google.common.collect.Iterables;

final class RpcServiceMetadata {
    private final Set<Class<? extends BaseIdentity>> contexts = new HashSet<>();
    private final Map<String, RpcMetadata> rpcMethods = new HashMap<>();
    private final Iterable<Class<? extends BaseIdentity>> roContexts = Iterables.unmodifiableIterable(contexts);

    public Iterable<Class<? extends BaseIdentity>> getContexts() {
        return roContexts;
    }

    public RpcMetadata getRpcMethod(final String name) {
        return rpcMethods.get(name);
    }

    public void addContext(final Class<? extends BaseIdentity> context) {
        contexts.add(context);
    }

    public void addRpcMethod(final String name, final RpcMetadata routingPair) {
        rpcMethods.put(name, routingPair);
    }
}
