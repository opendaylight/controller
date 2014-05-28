/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen;

import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.binding.RpcService;

public final class RuntimeCodeSpecification {
    public final static String DIRECT_PROXY_SUFFIX = "DirectProxy";
    public final static String INVOKER_SUFFIX = "ListenerInvoker";
    public final static String ROUTER_SUFFIX = "Router";

    public final static String DELEGATE_FIELD = "_delegate";
    public final static String ROUTING_TABLE_FIELD_PREFIX = "_routes_";

    private RuntimeCodeSpecification() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns a name for generated interface
     */
    private static String getGeneratedName(final Class<? extends Object> cls, final String suffix) {
        return cls.getName() + "$$Broker$" + suffix;
    }

    public static String getInvokerName(final Class<? extends NotificationListener> listener) {
        return getGeneratedName(listener, RuntimeCodeSpecification.INVOKER_SUFFIX);
    }

    /**
     * Returns a name for DirectProxy implementation
     */
    public static String getDirectProxyName(final Class<? extends RpcService> base) {
        return getGeneratedName(base, RuntimeCodeSpecification.DIRECT_PROXY_SUFFIX);
    }

    /**
     * Returns a name for Router implementation
     */
    public static String getRouterName(final Class<? extends RpcService> base) {
        return getGeneratedName(base, RuntimeCodeSpecification.ROUTER_SUFFIX);
    }

    /**
     * Returns a field name for specified routing context
     */
    public static String getRoutingTableField(final Class<? extends BaseIdentity> routingContext) {
        return "_routes_" + routingContext.getSimpleName();
    }
}
