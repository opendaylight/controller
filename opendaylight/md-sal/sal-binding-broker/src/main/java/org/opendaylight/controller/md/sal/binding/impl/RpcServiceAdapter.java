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

    RpcServiceAdapter(Class<? extends RpcService> type, ImmutableMap<Method, SchemaPath> rpcNames, InvocationDelegate delegate) {
        this.rpcNames = rpcNames;
        this.type = type;
        this.delegate = delegate;
        this.proxy = (RpcService) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, this);
    }

    RpcService getProxy() {
        return proxy;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        SchemaPath rpc = rpcNames.get(method);
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

    private static boolean isObjectMethod(Method m) {
        switch (m.getName()) {
        case "toString":
            return (m.getReturnType() == String.class && m.getParameterTypes().length == 0);
        case "hashCode":
            return (m.getReturnType() == int.class && m.getParameterTypes().length == 0);
        case "equals":
            return (m.getReturnType() == boolean.class && m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == Object.class);
        }
        return false;
    }

    private Object callObjectMethod(Object self, Method m, Object[] args) {
        switch (m.getName()) {
        case "toString":
            return type.getName() + "$Adapter{delegate=" + delegate.toString()+"}";
        case "hashCode":
            return System.identityHashCode(self);
        case "equals":
            return (self == args[0]);
        }
        return null;
    }
}
