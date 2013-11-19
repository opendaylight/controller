/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen

import java.util.Map

import org.opendaylight.yangtools.yang.binding.BaseIdentity
import org.opendaylight.yangtools.yang.binding.RpcService
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

import static extension org.opendaylight.controller.sal.binding.codegen.RuntimeCodeSpecification.*

class RuntimeCodeHelper {
    /**
     * Helper method to return delegate from ManagedDirectedProxy with use of reflection.
     * 
     * Note: This method uses reflection, but access to delegate field should be 
     * avoided and called only if neccessary.
     * 
     */
    public static def <T extends RpcService> getDelegate(RpcService proxy) {
        val field = proxy.class.getField(DELEGATE_FIELD)
        if (field == null) throw new UnsupportedOperationException("Unable to get delegate from proxy");
        return field.get(proxy) as T
    }

    /**
     * Helper method to set delegate to ManagedDirectedProxy with use of reflection.
     * 
     * Note: This method uses reflection, but setting delegate field should not occur too much
     * to introduce any significant performance hits.
     * 
     */
    public static def void setDelegate(RpcService proxy, RpcService delegate) {
        val field = proxy.class.getField(DELEGATE_FIELD)
        if (field == null) throw new UnsupportedOperationException("Unable to set delegate to proxy");
        if (delegate == null || field.type.isAssignableFrom(delegate.class)) {
            field.set(proxy, delegate)
        } else
            throw new IllegalArgumentException("delegate class is not assignable to proxy");
    }
    
        /**
     * Helper method to set delegate to ManagedDirectedProxy with use of reflection.
     * 
     * Note: This method uses reflection, but setting delegate field should not occur too much
     * to introduce any significant performance hits.
     * 
     */
    public static def void setDelegate(Object proxy, Object delegate) {
        val field = proxy.class.getField(DELEGATE_FIELD)
        if (field == null) throw new UnsupportedOperationException("Unable to set delegate to proxy");
        if (delegate == null || field.type.isAssignableFrom(delegate.class)) {
            field.set(proxy, delegate)
        } else
            throw new IllegalArgumentException("delegate class is not assignable to proxy");
    }
    

    public static def Map<InstanceIdentifier<?>, ? extends RpcService> getRoutingTable(RpcService target,
        Class<? extends BaseIdentity> tableClass) {
        val field = target.class.getField(tableClass.routingTableField)
        if (field == null) throw new UnsupportedOperationException(
            "Unable to get routing table. Table field does not exists");
        return field.get(target) as Map<InstanceIdentifier<? extends Object>, ? extends RpcService>;
    }

    public static def void setRoutingTable(RpcService target, Class<? extends BaseIdentity> tableClass,
        Map<InstanceIdentifier<?>, ? extends RpcService> routingTable) {
         val field = target.class.getField(tableClass.routingTableField)
        if (field == null) throw new UnsupportedOperationException(
            "Unable to set routing table. Table field does not exists");
        field.set(target,routingTable);
        
    }

}
