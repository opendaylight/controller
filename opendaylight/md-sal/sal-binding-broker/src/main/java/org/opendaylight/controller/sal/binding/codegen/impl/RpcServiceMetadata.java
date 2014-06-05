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

import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.DataContainer;

final class RpcServiceMetadata {
    private final HashMap<Class<? extends DataContainer>, RpcMetadata> rpcInputs = new HashMap<>();
    private final HashSet<Class<? extends DataContainer>> supportedInputs = new HashSet<>();
    private final HashSet<Class<? extends BaseIdentity>> contexts = new HashSet<>();
    private final HashMap<String, RpcMetadata> rpcMethods = new HashMap<>();

    public HashSet<Class<? extends BaseIdentity>> getContexts() {
        return this.contexts;
    }

    public HashMap<String, RpcMetadata> getRpcMethods() {
        return this.rpcMethods;
    }

    public HashMap<Class<? extends DataContainer>, RpcMetadata> getRpcInputs() {
        return this.rpcInputs;
    }

    public HashSet<Class<? extends DataContainer>> getSupportedInputs() {
        return this.supportedInputs;
    }
}
