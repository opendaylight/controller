/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.rpc;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry.Rpc;

import java.util.Map;

public final class ModuleRpcs {

    Map<String, String> yangToJavaNames = Maps.newHashMap();
    Map<String, Map<String, InstanceRuntimeRpc>> rpcMapping = Maps.newHashMap();

    public void addNameMapping(RuntimeBeanEntry runtimeEntry) {
        String yangName = runtimeEntry.getYangName();
        Preconditions.checkState(yangToJavaNames.containsKey(yangName) == false,
                "RuntimeBean %s found twice in same namespace", yangName);
        yangToJavaNames.put(yangName, runtimeEntry.getJavaNamePrefix());
    }

    public void addRpc(RuntimeBeanEntry runtimeEntry, Rpc rpc) {
        String yangName = runtimeEntry.getYangName();
        Map<String, InstanceRuntimeRpc> map = rpcMapping.get(yangName);
        if (map == null) {
            map = Maps.newHashMap();
            rpcMapping.put(yangName, map);
        }

        Preconditions.checkState(map.containsKey(rpc.getYangName()) == false, "Rpc %s for runtime bean %s added twice",
                rpc.getYangName(), yangName);
        map.put(rpc.getYangName(), new InstanceRuntimeRpc(rpc));
    }

    public String getRbeJavaName(String yangName) {
        String javaName = yangToJavaNames.get(yangName);
        Preconditions.checkState(javaName != null,
                "No runtime bean entry found under yang name %s, available yang names %s", yangName,
                yangToJavaNames.keySet());
        return javaName;
    }

    public InstanceRuntimeRpc getRpc(String rbeName, String rpcName) {
        Map<String, InstanceRuntimeRpc> rpcs = rpcMapping.get(rbeName);
        Preconditions.checkState(rpcs != null, "No rpcs found for runtime bean %s", rbeName);
        InstanceRuntimeRpc rpc = rpcs.get(rpcName);
        Preconditions.checkState(rpc != null, "No rpc found for runtime bean %s with name %s", rbeName, rpcName);
        return rpc;
    }
}
