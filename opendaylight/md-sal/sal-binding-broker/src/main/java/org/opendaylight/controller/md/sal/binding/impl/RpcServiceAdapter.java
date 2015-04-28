/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

class RpcServiceAdapter implements InvocationHandler {

    interface InvocationDelegate {

        ListenableFuture<RpcResult<?>> invoke(SchemaPath rpc, DataObject dataObject);

    }

    private final RpcService proxy;
    private final ImmutableMap<Method,SchemaPath> rpcNames;
    private final Class<? extends RpcService> type;
    private final InvocationDelegate delegate;

    RpcServiceAdapter(final Class<? extends RpcService> type, final ImmutableMap<Method, SchemaPath> rpcNames, final InvocationDelegate delegate) {
        this.rpcNames = rpcNames;
        this.type = type;
        this.delegate = delegate;
        this.proxy = (RpcService) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, this);
    }

    RpcService getProxy() {
        return proxy;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {

        final SchemaPath rpc = rpcNames.get(method);
        if(rpc != null) {
            if(method.getParameterTypes().length == 0) {
                return delegate.invoke(rpc, null);
            }
            if(args.length != 1) {
                throw new IllegalArgumentException("Input must be provided.");
            }
            return delegate.invoke(rpc,(DataObject) args[0]);
        }

        if(isObjectMethod(method)) {
            return callObjectMethod(proxy, method, args);
        }
        throw new UnsupportedOperationException("Method " + method.toString() + "is unsupported.");
    }

    private static boolean isObjectMethod(final Method m) {
        switch (m.getName()) {
            case "toString":
                return (String.class.equals(m.getReturnType()) && m.getParameterTypes().length == 0);
            case "hashCode":
                return (int.class.equals(m.getReturnType()) && m.getParameterTypes().length == 0);
            case "equals":
                return (boolean.class.equals(m.getReturnType()) && m.getParameterTypes().length == 1 && Object.class.equals(m.getParameterTypes()[0]));
            default:
                return false;
        }
    }

    private Object callObjectMethod(final Object self, final Method m, final Object[] args) {
        switch (m.getName()) {
            case "toString":
                return type.getName() + "$Adapter{delegate=" + delegate.toString()+"}";
            case "hashCode":
                return System.identityHashCode(self);
            case "equals":
                return (self == args[0]);
            default:
                return null;
        }
    }
}
