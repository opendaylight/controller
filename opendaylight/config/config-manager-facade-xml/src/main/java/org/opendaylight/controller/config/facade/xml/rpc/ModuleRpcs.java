/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.rpc;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;
import org.opendaylight.controller.config.facade.xml.osgi.EnumResolver;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry.Rpc;

public final class ModuleRpcs {

    private final Map<String, String> yangToJavaNames = Maps.newHashMap();
    private final Map<String, Map<String, InstanceRuntimeRpc>> rpcMapping = Maps.newHashMap();
    private final EnumResolver enumResolver;

    public ModuleRpcs(final EnumResolver enumResolver) {
        this.enumResolver = enumResolver;
    }

    public void addNameMapping(RuntimeBeanEntry runtimeEntry) {
        String yangName = runtimeEntry.getYangName();
        Preconditions.checkState(!yangToJavaNames.containsKey(yangName),
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

        Preconditions.checkState(!map.containsKey(rpc.getYangName()), "Rpc %s for runtime bean %s added twice",
                rpc.getYangName(), yangName);
        map.put(rpc.getYangName(), new InstanceRuntimeRpc(rpc, enumResolver));
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
