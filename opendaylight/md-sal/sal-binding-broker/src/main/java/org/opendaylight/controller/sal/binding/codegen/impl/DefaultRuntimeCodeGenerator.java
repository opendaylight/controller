/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen.impl;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import java.lang.reflect.Method;
import java.util.Map;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.opendaylight.controller.sal.binding.codegen.RuntimeCodeSpecification;
import org.opendaylight.yangtools.sal.binding.generator.util.ClassGenerator;
import org.opendaylight.yangtools.sal.binding.generator.util.MethodGenerator;
import org.opendaylight.yangtools.util.ClassLoaderUtils;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.binding.RpcImplementation;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;

final class DefaultRuntimeCodeGenerator extends AbstractRuntimeCodeGenerator {

    DefaultRuntimeCodeGenerator(final ClassPool pool) {
        super(pool);
    }

    @Override
    protected <T extends RpcService> Supplier<T> directProxySupplier(final Class<T> serviceType) {
        return new Supplier<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T get() {
                final String proxyName = RuntimeCodeSpecification.getDirectProxyName(serviceType);

                final Class<?> potentialClass = ClassLoaderUtils.tryToLoadClassWithTCCL(proxyName);
                if (potentialClass != null) {
                    try {
                        return (T)potentialClass.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new IllegalStateException("Failed to instantiate class " + potentialClass.getName(), e);
                    }
                }

                final CtClass supertype = utils.asCtClass(serviceType);
                final String directProxyName = RuntimeCodeSpecification.getDirectProxyName(serviceType);

                final CtClass createdCls;
                try {
                    createdCls = utils.createClass(directProxyName, supertype, new ClassGenerator() {
                        @Override
                        public void process(final CtClass cls) throws CannotCompileException {
                            utils.field(cls, RuntimeCodeSpecification.DELEGATE_FIELD, serviceType);
                            utils.implementsType(cls, utils.asCtClass(RpcImplementation.class));
                            utils.implementMethodsFrom(cls, supertype, new MethodGenerator() {
                                @Override
                                public void process(final CtMethod method) throws CannotCompileException {
                                    final StringBuilder sb = new StringBuilder("\n");
                                    sb.append("{\n");
                                    sb.append("    if (").append(RuntimeCodeSpecification.DELEGATE_FIELD).append(" == null) {\n");
                                    sb.append("        throw new java.lang.IllegalStateException(\"No default provider is available\");\n");
                                    sb.append("    }\n");
                                    sb.append("    return ($r) ").append(RuntimeCodeSpecification.DELEGATE_FIELD).append('.').append(method.getName()).append("($$);\n");
                                    sb.append("}\n");
                                    method.setBody(sb.toString());
                                }
                            });

                            // FIXME: copy this one...
                            utils.implementMethodsFrom(cls, utils.asCtClass(RpcImplementation.class), new MethodGenerator() {
                                @Override
                                public void process(final CtMethod method) throws CannotCompileException {
                                    final StringBuilder sb = new StringBuilder("\n");
                                    sb.append("{\n");
                                    sb.append("    throw new java.lang.IllegalStateException(\"No provider is processing supplied message\");\n");
                                    sb.append("    return ($r) null;\n");
                                    sb.append("}\n");
                                    method.setBody(sb.toString());
                                }
                            });
                        }
                    });
                } catch (CannotCompileException e) {
                    throw new IllegalStateException("Failed to create class " + directProxyName, e);
                }

                final Class<?> c;
                try {
                    c = createdCls.toClass(serviceType.getClassLoader(), serviceType.getProtectionDomain());
                } catch (CannotCompileException e) {
                    throw new IllegalStateException(String.format("Failed to create class %s", createdCls), e);
                }

                try {
                    return (T) c.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new IllegalStateException(String.format("Failed to instantiated class %s", c), e);
                }
            }
        };
    }

    @Override
    protected <T extends RpcService> Supplier<T> routerSupplier(final Class<T> serviceType, final RpcServiceMetadata metadata) {
        return new Supplier<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T get() {
                final CtClass supertype = utils.asCtClass(serviceType);
                final String routerName = RuntimeCodeSpecification.getRouterName(serviceType);
                final Class<?> potentialClass = ClassLoaderUtils.tryToLoadClassWithTCCL(routerName);
                if (potentialClass != null) {
                    try {
                        return (T)potentialClass.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new IllegalStateException("Failed to instantiate class", e);
                    }
                }

                final CtClass targetCls;
                try {
                    targetCls = utils.createClass(routerName, supertype, new ClassGenerator() {
                        @Override
                        public void process(final CtClass cls) throws CannotCompileException {
                            utils.field(cls, RuntimeCodeSpecification.DELEGATE_FIELD, serviceType);
                            //utils.field(cls, REMOTE_INVOKER_FIELD,iface);
                            utils.implementsType(cls, utils.asCtClass(RpcImplementation.class));

                            for (final Class<? extends BaseIdentity> ctx : metadata.getContexts()) {
                                utils.field(cls, RuntimeCodeSpecification.getRoutingTableField(ctx), Map.class);
                            }

                            utils.implementMethodsFrom(cls, supertype, new MethodGenerator() {
                                @Override
                                public void process(final CtMethod method) throws CannotCompileException {
                                    final int ptl;
                                    try {
                                        ptl = method.getParameterTypes().length;
                                    } catch (NotFoundException e) {
                                        throw new CannotCompileException(e);
                                    }
                                    final StringBuilder sb = new StringBuilder();

                                    switch (ptl) {
                                    case 0:
                                        sb.append("return ($r) ").append(RuntimeCodeSpecification.DELEGATE_FIELD).append('.').append(method.getName()).append("($$);");
                                        break;
                                    case 1:
                                        final RpcMetadata rpcMeta = metadata.getRpcMethod(method.getName());
                                        final String rtGetter = rpcMeta.getInputRouteGetter().getName();
                                        final String stName = supertype.getName();

                                        sb.append('\n');
                                        sb.append("{\n");
                                        sb.append("    if ($1 == null) {\n");
                                        sb.append("        throw new IllegalArgumentException(\"RPC input must not be null and must contain a value for field ").append(rtGetter).append("\");\n");
                                        sb.append("    }\n");
                                        sb.append("    if ($1.").append(rtGetter).append("() == null) {\n");
                                        sb.append("        throw new IllegalArgumentException(\"Field ").append(rtGetter).append(" must not be null\");\n");
                                        sb.append("    }\n");

                                        sb.append("    final org.opendaylight.yangtools.yang.binding.InstanceIdentifier identifier = $1.").append(rtGetter).append("()");
                                        if (rpcMeta.isRouteEncapsulated()) {
                                            sb.append(".getValue()");
                                        }
                                        sb.append(";\n");

                                        sb.append("    ").append(supertype.getName()).append(" instance = (").append(stName).append(") ").append(RuntimeCodeSpecification.getRoutingTableField(rpcMeta.getContext())).append(".get(identifier);\n");
                                        sb.append("    if (instance == null) {\n");
                                        sb.append("        instance = ").append(RuntimeCodeSpecification.DELEGATE_FIELD).append(";\n");
                                        sb.append("    }\n");

                                        sb.append("    if (instance == null) {\n");
                                        sb.append("        throw new java.lang.IllegalStateException(\"No routable provider is processing routed message for \" + String.valueOf(identifier));\n");
                                        sb.append("    }\n");
                                        sb.append("    return ($r) instance.").append(method.getName()).append("($$);\n");
                                        sb.append('}');
                                        break;
                                    default:
                                        throw new CannotCompileException(String.format("Unsupported parameters length %s", ptl));
                                    }

                                    method.setBody(sb.toString());
                                }
                            });

                            // FIXME: move this into a template class
                            utils.implementMethodsFrom(cls, utils.asCtClass(RpcImplementation.class), new MethodGenerator() {
                                @Override
                                public void process(final CtMethod method) throws CannotCompileException {
                                    final StringBuilder sb = new StringBuilder("\n");
                                    sb.append("{\n");
                                    sb.append("    throw new java.lang.IllegalStateException(\"No provider is processing supplied message\");\n");
                                    sb.append("    return ($r) null;\n");
                                    sb.append("}\n");

                                    method.setBody(sb.toString());
                                }
                            });
                        }
                    });
                } catch (CannotCompileException e) {
                    throw new IllegalStateException("Failed to create class " + routerName, e);
                }

                final Class<?> c;
                try {
                    c = targetCls.toClass(serviceType.getClassLoader(), serviceType.getProtectionDomain());
                } catch (CannotCompileException e) {
                    throw new IllegalStateException(String.format("Failed to compile class %s", targetCls), e);
                }

                try {
                    return (T)c.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new IllegalStateException(String.format("Failed to instantiate class %s", c), e);
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    protected RuntimeGeneratedInvokerPrototype generateListenerInvoker(final Class<? extends NotificationListener> listenerType) {
        final String invokerName = RuntimeCodeSpecification.getInvokerName(listenerType);
        final CtClass targetCls;

        // Builder for a set of supported types. Filled while the target class is being generated
        final Builder<Class<? extends Notification>> b = ImmutableSet.builder();

        try {
            targetCls = utils.createClass(invokerName, getBrokerNotificationListener(), new ClassGenerator() {
                @Override
                public void process(final CtClass cls) throws CannotCompileException {
                    utils.field(cls, RuntimeCodeSpecification.DELEGATE_FIELD, listenerType);
                    utils.implementMethodsFrom(cls, getBrokerNotificationListener(), new MethodGenerator() {
                        @Override
                        public void process(final CtMethod method) throws CannotCompileException {
                            final StringBuilder sb = new StringBuilder("\n");

                            sb.append("{\n");

                            for (Method m : listenerType.getMethods()) {
                                if (BindingReflections.isNotificationCallback(m)) {
                                    final Class<?> argType = m.getParameterTypes()[0];

                                    // populates builder above
                                    b.add((Class<? extends Notification>) argType);

                                    sb.append("    if ($1 instanceof ").append(argType.getName()).append(") {\n");
                                    sb.append("        ").append(RuntimeCodeSpecification.DELEGATE_FIELD).append('.').append(m.getName()).append("((").append(argType.getName()).append(") $1);\n");
                                    sb.append("        return null;\n");
                                    sb.append("    } else ");
                                }
                            }

                            sb.append("    return null;\n");
                            sb.append("}\n");
                            method.setBody(sb.toString());
                        }
                    });
                }
            });
        } catch (CannotCompileException e) {
            throw new IllegalStateException("Failed to create class " + invokerName, e);
        }

        final Class<?> finalClass;
        try {
            finalClass = targetCls.toClass(listenerType.getClassLoader(), listenerType.getProtectionDomain());
        } catch (CannotCompileException e) {
            throw new IllegalStateException(String.format("Failed to compile class %s", targetCls), e);
        }

        return new RuntimeGeneratedInvokerPrototype(b.build(), (Class<? extends org.opendaylight.controller.sal.binding.api.NotificationListener<?>>) finalClass);
    }
}
