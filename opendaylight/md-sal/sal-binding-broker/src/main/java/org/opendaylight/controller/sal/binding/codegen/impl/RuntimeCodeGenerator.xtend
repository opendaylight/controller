/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen.impl

import java.util.Map
import javassist.ClassPool
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.Notification
import org.opendaylight.yangtools.yang.binding.RpcImplementation
import org.opendaylight.yangtools.yang.binding.util.BindingReflections
import org.opendaylight.yangtools.yang.binding.util.ClassLoaderUtils

import static extension org.opendaylight.controller.sal.binding.codegen.RuntimeCodeSpecification.*
import org.opendaylight.yangtools.yang.binding.RpcService

class RuntimeCodeGenerator extends AbstractRuntimeCodeGenerator {

    new(ClassPool pool) {
        super(pool)
    }

    override directProxySupplier(Class iface) {
        return [|
            val proxyName = iface.directProxyName;
            val potentialClass = ClassLoaderUtils.tryToLoadClassWithTCCL(proxyName)
            if(potentialClass != null) {
                return potentialClass.newInstance as RpcService;
            }
            val supertype = iface.asCtClass
            val createdCls = createClass(iface.directProxyName, supertype) [
                field(DELEGATE_FIELD, iface);
                implementsType(RpcImplementation.asCtClass)
                implementMethodsFrom(supertype) [
                    body = '''
                    {
                        if(«DELEGATE_FIELD» == null) {
                            throw new java.lang.IllegalStateException("No default provider is available");
                        }
                        return ($r) «DELEGATE_FIELD».«it.name»($$);
                    }
                    '''
                ]
                implementMethodsFrom(RpcImplementation.asCtClass) [
                    body = '''
                    {
                        throw new java.lang.IllegalStateException("No provider is processing supplied message");
                        return ($r) null;
                    }
                    '''
                ]
            ]
            return createdCls.toClass(iface.classLoader).newInstance as RpcService
        ]
    }

    override routerSupplier(Class iface, RpcServiceMetadata metadata) {
        return [ |
            val supertype = iface.asCtClass
            val routerName = iface.routerName;
            val potentialClass = ClassLoaderUtils.tryToLoadClassWithTCCL(routerName)
            if(potentialClass != null) {
                return potentialClass.newInstance as RpcService;
            }

            val targetCls = createClass(iface.routerName, supertype) [


                field(DELEGATE_FIELD, iface)
                //field(REMOTE_INVOKER_FIELD,iface);
                implementsType(RpcImplementation.asCtClass)

                for (ctx : metadata.contexts) {
                    field(ctx.routingTableField, Map)
                }
                implementMethodsFrom(supertype) [
                    if (parameterTypes.size === 1) {
                        val rpcMeta = metadata.getRpcMethod(name);
                        val bodyTmp = '''
                        {
                            if($1 == null) {
                                throw new IllegalArgumentException("RPC input must not be null and must contain a value for field «rpcMeta.inputRouteGetter.name»");
                            }
                            if($1.«rpcMeta.inputRouteGetter.name»() == null) {
                                throw new IllegalArgumentException("Field «rpcMeta.inputRouteGetter.name» must not be null");
                            }
                            final «InstanceIdentifier.name» identifier = $1.«rpcMeta.inputRouteGetter.name»()«IF rpcMeta.
                            routeEncapsulated».getValue()«ENDIF»;
                            «supertype.name» instance = («supertype.name») «rpcMeta.context.routingTableField».get(identifier);
                            if(instance == null) {
                               instance = «DELEGATE_FIELD»;
                            }
                            if(instance == null) {
                                throw new java.lang.IllegalStateException("No routable provider is processing routed message for " + String.valueOf(identifier));
                            }
                            return ($r) instance.«it.name»($$);
                        }'''
                        body = bodyTmp
                    } else if (parameterTypes.size === 0) {
                        body = '''return ($r) «DELEGATE_FIELD».«it.name»($$);'''
                    }
                ]
                implementMethodsFrom(RpcImplementation.asCtClass) [
                    body = '''
                    {
                        throw new java.lang.IllegalStateException("No provider is processing supplied message");
                        return ($r) null;
                    }
                    '''
                ]
            ]
            return  targetCls.toClass(iface.classLoader,iface.protectionDomain).newInstance as RpcService
        ];
    }

    override generateListenerInvoker(Class iface) {
        val callbacks = iface.methods.filter[BindingReflections.isNotificationCallback(it)]

        val supportedNotification = callbacks.map[parameterTypes.get(0) as Class<? extends Notification>].toSet;

        val targetCls = createClass(iface.invokerName, brokerNotificationListener) [
            field(DELEGATE_FIELD, iface)
            implementMethodsFrom(brokerNotificationListener) [
                body = '''
                    {
                        «FOR callback : callbacks SEPARATOR " else "»
                            «val cls = callback.parameterTypes.get(0).name»
                                if($1 instanceof «cls») {
                                    «DELEGATE_FIELD».«callback.name»((«cls») $1);
                                    return null;
                                }
                        «ENDFOR»
                        return null;
                    }
                '''
            ]
        ]
        val finalClass = targetCls.toClass(iface.classLoader, iface.protectionDomain)
        return new RuntimeGeneratedInvokerPrototype(supportedNotification,
            finalClass as Class<? extends org.opendaylight.controller.sal.binding.api.NotificationListener<?>>);
    }
}
