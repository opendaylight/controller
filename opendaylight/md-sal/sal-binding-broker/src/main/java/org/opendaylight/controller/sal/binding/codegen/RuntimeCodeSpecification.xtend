/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen

import org.opendaylight.yangtools.yang.binding.RpcService
import org.opendaylight.yangtools.yang.binding.BaseIdentity
import org.opendaylight.yangtools.yang.binding.NotificationListener

/**
 * 
 * 
 */
class RuntimeCodeSpecification {

    //public static val PACKAGE_PREFIX = "_gen.";

    public static val DIRECT_PROXY_SUFFIX = "DirectProxy";
    public static val ROUTER_SUFFIX = "Router";
    public static val INVOKER_SUFFIX = "ListenerInvoker";

    public static val DELEGATE_FIELD = "_delegate"
    public static val ROUTING_TABLE_FIELD_PREFIX = "_routes_"

    public static def getInvokerName(Class<? extends NotificationListener> listener) {
        getGeneratedName(listener, INVOKER_SUFFIX);
    }

    /**
     * Returns a name for DirectProxy implementation
     * 
     * 
     */
    public static def getDirectProxyName(Class<? extends RpcService> base) {
        getGeneratedName(base, DIRECT_PROXY_SUFFIX);
    }

    /**
     * Returns a name for Router implementation
     * 
     */
    public static def getRouterName(Class<? extends RpcService> base) {
        getGeneratedName(base, ROUTER_SUFFIX);
    }

    /**
     * Returns a name for generated interface
     * 
     */
    public static def getGeneratedName(Class<?> cls, String suffix) {
        '''«cls.name»$$Broker$«suffix»'''.toString()
    }

    /**
     * Returns a field name for specified routing context
     * 
     */
    public static def getRoutingTableField(Class<? extends BaseIdentity> routingContext) {
        return '''_routes_«routingContext.simpleName»'''.toString;
    }
}
