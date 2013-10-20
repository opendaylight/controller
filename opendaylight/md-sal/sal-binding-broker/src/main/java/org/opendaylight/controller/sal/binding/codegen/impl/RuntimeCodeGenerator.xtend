/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen.impl

import javassist.ClassPool
import org.opendaylight.yangtools.yang.binding.RpcService

import javassist.CtClass
import static com.google.common.base.Preconditions.*

import javassist.CtField
import javassist.Modifier
import javassist.CtMethod
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.annotations.RoutingContext
import org.opendaylight.yangtools.yang.binding.BaseIdentity

import java.util.Map
import java.util.HashMap
import javassist.NotFoundException
import javassist.LoaderClassPath
import org.opendaylight.controller.sal.binding.codegen.impl.JavassistUtils.MethodGenerator
import org.opendaylight.controller.sal.binding.codegen.impl.JavassistUtils.ClassGenerator
import org.opendaylight.yangtools.yang.binding.NotificationListener
import org.opendaylight.yangtools.yang.binding.Notification
import java.util.Arrays

import static extension org.opendaylight.controller.sal.binding.codegen.YangtoolsMappingHelper.*
import static extension org.opendaylight.controller.sal.binding.codegen.RuntimeCodeSpecification.*
import java.util.HashSet
import java.io.ObjectOutputStream.PutField
import static org.opendaylight.controller.sal.binding.impl.osgi.ClassLoaderUtils.*
import javax.xml.ws.spi.Invoker
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory.NotificationInvoker
import java.util.Set
import java.util.Collections
import org.opendaylight.controller.sal.binding.codegen.RuntimeCodeHelper
import java.util.WeakHashMap
import javassist.ClassClassPath

class RuntimeCodeGenerator implements org.opendaylight.controller.sal.binding.codegen.RuntimeCodeGenerator, NotificationInvokerFactory {

    val CtClass BROKER_NOTIFICATION_LISTENER;
    val ClassPool classPool;
    val Map<Class<? extends NotificationListener>, RuntimeGeneratedInvokerPrototype> invokerClasses;

    public new(ClassPool pool) {
        classPool = pool;
        invokerClasses = new WeakHashMap();
        BROKER_NOTIFICATION_LISTENER = org.opendaylight.controller.sal.binding.api.NotificationListener.asCtClass;
    }

    override <T extends RpcService> getDirectProxyFor(Class<T> iface) {
        val supertype = iface.asCtClass
        val targetCls = createClass(iface.directProxyName, supertype) [
            field(DELEGATE_FIELD, iface);
            implementMethodsFrom(supertype) [
                body = '''return ($r) «DELEGATE_FIELD».«it.name»($$);'''
            ]
        ]
        return targetCls.toClass(iface.classLoader).newInstance as T
    }

    override <T extends RpcService> getRouterFor(Class<T> iface) {
        val contexts = new HashSet<Class<? extends BaseIdentity>>

        val instance = <T>withClassLoader(iface.classLoader) [ |
            val supertype = iface.asCtClass
            val targetCls = createClass(iface.routerName, supertype) [
                //field(ROUTING_TABLE_FIELD,Map)
                field(DELEGATE_FIELD, iface)
                val ctxMap = new HashMap<String, Class<? extends BaseIdentity>>();
                // We search for routing pairs and add fields
                supertype.methods.filter[declaringClass == supertype && parameterTypes.size === 1].forEach [ method |
                    val routingPair = method.routingContextInput;
                    if (routingPair !== null) {
                        ctxMap.put(routingPair.context.routingTableField, routingPair.context);
                        contexts.add(routingPair.context)
                    }
                ]
                for (ctx : ctxMap.entrySet) {
                    field(ctx.key, Map)
                }
                implementMethodsFrom(supertype) [
                    if (parameterTypes.size === 1) {
                        val routingPair = routingContextInput;
                        val bodyTmp = '''
                        {
                            final «InstanceIdentifier.name» identifier = $1.«routingPair.getter.name»()«IF routingPair.
                            encapsulated».getValue()«ENDIF»;
                            «supertype.name» instance = («supertype.name») «routingPair.context.routingTableField».get(identifier);
                            if(instance == null) {
                               instance = «DELEGATE_FIELD»;
                            }
                            if(instance == null) {
                                throw new java.lang.IllegalStateException("No provider is processing supplied message");
                            }
                            return ($r) instance.«it.name»($$);
                        }'''
                        body = bodyTmp
                    } else if (parameterTypes.size === 0) {
                        body = '''return ($r) «DELEGATE_FIELD».«it.name»($$);'''
                    }
                ]
            ]
            return targetCls.toClass(iface.classLoader).newInstance as T
        ];
        return new RpcRouterCodegenInstance(iface, instance, contexts);
    }

    protected def generateListenerInvoker(Class<? extends NotificationListener> iface) {
        val callbacks = iface.methods.filter[notificationCallback]

        val supportedNotification = callbacks.map[parameterTypes.get(0) as Class<? extends Notification>].toSet;

        val targetCls = createClass(iface.invokerName,BROKER_NOTIFICATION_LISTENER ) [
            field(DELEGATE_FIELD, iface)
            implementMethodsFrom(BROKER_NOTIFICATION_LISTENER) [
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
        val finalClass = targetCls.toClass(iface.classLoader,iface.protectionDomain)
        return new RuntimeGeneratedInvokerPrototype(supportedNotification,
            finalClass as Class<? extends org.opendaylight.controller.sal.binding.api.NotificationListener>);
    }

    def void method(CtClass it, Class<?> returnType, String name, Class<?> parameter, MethodGenerator function1) {
        val method = new CtMethod(returnType.asCtClass, name, Arrays.asList(parameter.asCtClass), it);
        function1.process(method);
        it.addMethod(method);
    }

    private def routingContextInput(CtMethod method) {
        val inputClass = method.parameterTypes.get(0);
        return inputClass.contextInstance;
    }

    private def RoutingPair getContextInstance(CtClass dataClass) {
        for (method : dataClass.methods) {
            if (method.name.startsWith("get") && method.parameterTypes.size === 0) {
                for (annotation : method.availableAnnotations) {
                    if (annotation instanceof RoutingContext) {
                        val encapsulated = !method.returnType.equals(InstanceIdentifier.asCtClass);

                        return new RoutingPair((annotation as RoutingContext).value, method, encapsulated);
                    }
                }
            }
        }
        for (iface : dataClass.interfaces) {
            val ret = getContextInstance(iface);
            if(ret != null) return ret;
        }
        return null;
    }

    private def void implementMethodsFrom(CtClass target, CtClass source, MethodGenerator function1) {
        for (method : source.methods) {
            if (method.declaringClass == source) {
                val redeclaredMethod = new CtMethod(method, target, null);
                function1.process(redeclaredMethod);
                target.addMethod(redeclaredMethod);
            }
        }
    }

    private def CtClass createClass(String fqn, ClassGenerator cls) {
        val target = classPool.makeClass(fqn);
        cls.process(target);
        return target;
    }

    private def CtClass createClass(String fqn, CtClass superInterface, ClassGenerator cls) {
        val target = classPool.makeClass(fqn);
        target.implementsType(superInterface);
        cls.process(target);
        return target;
    }

    private def void implementsType(CtClass it, CtClass supertype) {
        checkArgument(supertype.interface, "Supertype must be interface");
        addInterface(supertype);
    }

    private def asCtClass(Class<?> class1) {
        classPool.get(class1);
    }

    private def CtField field(CtClass it, String name, Class<?> returnValue) {
        val field = new CtField(returnValue.asCtClass, name, it);
        field.modifiers = Modifier.PUBLIC
        addField(field);
        return field;
    }

    def get(ClassPool pool, Class<?> cls) {
        try {
            return pool.get(cls.name)
        } catch (NotFoundException e) {
            pool.appendClassPath(new LoaderClassPath(cls.classLoader));
            try {
                return pool.get(cls.name)

            } catch (NotFoundException ef) {
                pool.appendClassPath(new ClassClassPath(cls));
                return pool.get(cls.name)
            }
        }
    }

    override getInvokerFactory() {
        return this;
    }

    override invokerFor(NotificationListener instance) {
        val cls = instance.class
        val prototype = resolveInvokerClass(cls);
        
        return new RuntimeGeneratedInvoker(instance,prototype)
    }

    def resolveInvokerClass(Class<? extends NotificationListener> class1) {
        val invoker = invokerClasses.get(class1);
        if (invoker !== null) {
            return invoker;
        }
        val newInvoker = generateListenerInvoker(class1);
        invokerClasses.put(class1, newInvoker);
        return newInvoker
    }
}

@Data
class RuntimeGeneratedInvoker implements NotificationInvoker {
    
    @Property
    val NotificationListener delegate;

    
    @Property
    var org.opendaylight.controller.sal.binding.api.NotificationListener invocationProxy;

    @Property
    var RuntimeGeneratedInvokerPrototype prototype;

    new(NotificationListener delegate,RuntimeGeneratedInvokerPrototype prototype) {
        _delegate = delegate;
        _prototype = prototype;
        _invocationProxy = prototype.protoClass.newInstance;
        RuntimeCodeHelper.setDelegate(_invocationProxy, delegate);
    }

    override getSupportedNotifications() {
        prototype.supportedNotifications;
    }

    override close() {
        
    }
}

@Data
class RuntimeGeneratedInvokerPrototype {

    @Property
    val Set<Class<? extends Notification>> supportedNotifications;

    @Property
    val Class<? extends org.opendaylight.controller.sal.binding.api.NotificationListener> protoClass;
}
