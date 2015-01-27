/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen.impl;

import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import java.util.Map;
import java.util.WeakHashMap;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.sal.binding.api.rpc.RpcRouter;
import org.opendaylight.controller.sal.binding.codegen.RpcIsNotRoutedException;
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.util.ClassLoaderUtils;
import org.opendaylight.yangtools.yang.binding.BindingMapping;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.binding.annotations.RoutingContext;

abstract class AbstractRuntimeCodeGenerator implements org.opendaylight.controller.sal.binding.codegen.RuntimeCodeGenerator, NotificationInvokerFactory {
    @GuardedBy("this")
    private final Map<Class<? extends NotificationListener>, RuntimeGeneratedInvokerPrototype> invokerClasses = new WeakHashMap<>();
    private final CtClass brokerNotificationListener;
    protected final JavassistUtils utils;

    protected AbstractRuntimeCodeGenerator(final ClassPool pool) {
        utils = JavassistUtils.forClassPool(pool);

        /*
         * Make sure Javassist ClassPool sees the classloader of RpcService
         */
        utils.ensureClassLoader(RpcService.class);

        brokerNotificationListener = utils.asCtClass(org.opendaylight.controller.sal.binding.api.NotificationListener.class);
    }

    protected final CtClass getBrokerNotificationListener() {
        return brokerNotificationListener;
    }

    protected abstract RuntimeGeneratedInvokerPrototype generateListenerInvoker(Class<? extends NotificationListener> cls);
    protected abstract <T extends RpcService> Supplier<T> directProxySupplier(final Class<T> serviceType);
    protected abstract <T extends RpcService> Supplier<T> routerSupplier(final Class<T> serviceType, RpcServiceMetadata metadata);

    private RpcServiceMetadata getRpcMetadata(final CtClass iface) throws ClassNotFoundException, NotFoundException, RpcIsNotRoutedException {
        final RpcServiceMetadata metadata = new RpcServiceMetadata();

        for (CtMethod method : iface.getMethods()) {
            if (isRpcMethodWithInput(iface, method)) {
                final RpcMetadata routingPair = getRpcMetadata(method);
                if (routingPair != null) {
                    metadata.addContext(routingPair.getContext());
                    metadata.addRpcMethod(method.getName(), routingPair);

                    /*
                     * Force-load the RPC class representing the "input" of this RPC.
                     *
                     * FIXME: this is pre-existing side-effect of the original code, which
                     *        kept a reference to the loaded class, but it did not use it.
                     *
                     *        There was no explanation as to why forcing this load was
                     *        necessary. As far as I can tell now is that it forces the
                     *        resolution of method arguments, which would (according to
                     *        my reading of JLS) occur only when the method is invoked via
                     *        binding-aware class action, not when coming from
                     *        binding-independent world. Whether that makes sense or not,
                     *        remains to be investigated.
                     */
                    Thread.currentThread().getContextClassLoader().loadClass(routingPair.getInputType().getName());
                } else {
                    throw new RpcIsNotRoutedException(String.format("RPC %s from %s is not routed", method.getName(), iface.getName()));
                }
            }
        }

        return metadata;
    }


    private boolean isRpcMethodWithInput(final CtClass iface, final CtMethod method) throws NotFoundException {
        if(iface.equals(method.getDeclaringClass())
                && method.getParameterTypes().length == 1) {
            final CtClass onlyArg = method.getParameterTypes()[0];
            if(onlyArg.isInterface() && onlyArg.getName().endsWith(BindingMapping.RPC_INPUT_SUFFIX)) {
                return true;
            }
        }
        return false;
    }

    private RpcMetadata getRpcMetadata(final CtMethod method) throws NotFoundException {
        final CtClass inputClass = method.getParameterTypes()[0];
        return rpcMethodMetadata(inputClass, inputClass, method.getName());
    }

    private RpcMetadata rpcMethodMetadata(final CtClass dataClass, final CtClass inputClass, final String rpcMethod) throws NotFoundException {
        for (CtMethod method : dataClass.getMethods()) {
            if (method.getName().startsWith("get") && method.getParameterTypes().length == 0) {
                for (Object annotation : method.getAvailableAnnotations()) {
                    if (annotation instanceof RoutingContext) {
                        boolean encapsulated = !method.getReturnType().equals(utils.asCtClass(InstanceIdentifier.class));
                        return new RpcMetadata(rpcMethod, ((RoutingContext)annotation).value(), method, encapsulated, inputClass);
                    }
                }
            }
        }

        for (CtClass iface : dataClass.getInterfaces()) {
            final RpcMetadata ret = rpcMethodMetadata(iface, inputClass, rpcMethod);
            if(ret != null) {
                return ret;
            }
        }
        return null;
    }

    private synchronized RuntimeGeneratedInvokerPrototype resolveInvokerClass(final Class<? extends NotificationListener> cls) {
        RuntimeGeneratedInvokerPrototype invoker = invokerClasses.get(cls);
        if (invoker != null) {
            return invoker;
        }

        synchronized (utils) {
            invoker = ClassLoaderUtils.withClassLoader(cls.getClassLoader(), new Supplier<RuntimeGeneratedInvokerPrototype>() {
                @Override
                public RuntimeGeneratedInvokerPrototype get() {
                    return generateListenerInvoker(cls);
                }
            });
        }

        invokerClasses.put(cls, invoker);
        return invoker;
    }

    @Override
    public final NotificationInvokerFactory getInvokerFactory() {
        return this;
    }

    @Override
    public final <T extends RpcService> T getDirectProxyFor(final Class<T> serviceType) {
        synchronized (utils) {
            return ClassLoaderUtils.withClassLoader(serviceType.getClassLoader(), directProxySupplier(serviceType));
        }
    }

    @Override
    public final <T extends RpcService> RpcRouter<T> getRouterFor(final Class<T> serviceType, final String name) throws RpcIsNotRoutedException {
        final RpcServiceMetadata metadata = ClassLoaderUtils.withClassLoader(serviceType.getClassLoader(), new Supplier<RpcServiceMetadata>() {
            @Override
            public RpcServiceMetadata get() {
                try {
                    return getRpcMetadata(utils.asCtClass(serviceType));
                } catch (ClassNotFoundException | NotFoundException e) {
                    throw new IllegalStateException(String.format("Failed to load metadata for class %s", serviceType), e);
                }
            }
        });

        if (Iterables.isEmpty(metadata.getContexts())) {
            throw new RpcIsNotRoutedException("Service doesn't have routing context associated.");
        }

        synchronized (utils) {
            final T instance = ClassLoaderUtils.withClassLoader(serviceType.getClassLoader(), routerSupplier(serviceType, metadata));
            return new RpcRouterCodegenInstance<T>(name, serviceType, instance, metadata.getContexts());
        }
    }

    @Override
    public NotificationInvoker invokerFor(final NotificationListener instance) {
        final Class<? extends NotificationListener> cls = instance.getClass();
        final RuntimeGeneratedInvokerPrototype prototype = resolveInvokerClass(cls);

        try {
            return RuntimeGeneratedInvoker.create(instance, prototype);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(String.format("Failed to create invoker for %s", instance), e);
        }
    }
}
