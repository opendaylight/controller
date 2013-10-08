/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.rpc;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.runtimerpc.RuntimeRpcElementResolved;

import java.util.Map;

public class Rpcs {
    private final Map<String, Map<String, ModuleRpcs>> mappedRpcs;

    public Rpcs(Map<String, Map<String, ModuleRpcs>> mappedRpcs) {
        super();
        this.mappedRpcs = mappedRpcs;
    }

    public ModuleRpcs getRpcMapping(RuntimeRpcElementResolved id) {
        Map<String, ModuleRpcs> modules = mappedRpcs.get(id.getNamespace());
        Preconditions.checkState(modules != null, "No modules found for namespace %s", id.getNamespace());
        ModuleRpcs rpcMapping = modules.get(id.getModuleName());
        Preconditions.checkState(modules != null, "No module %s found for namespace %s", id.getModuleName(),
                id.getNamespace());

        return rpcMapping;
    }
}
