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
import static org.opendaylight.controller.sal.binding.impl.osgi.ClassLoaderUtils.*
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory.NotificationInvoker
import java.util.Set
import org.opendaylight.controller.sal.binding.codegen.RuntimeCodeHelper
import java.util.WeakHashMap
import javassist.ClassClassPath
import org.opendaylight.yangtools.yang.binding.annotations.QName
import org.opendaylight.yangtools.yang.binding.DataContainer
import org.opendaylight.yangtools.yang.binding.RpcImplementation

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

        val instance = <RpcRouterCodegenInstance<T>>withClassLoader(iface.classLoader) [ |
            val supertype = iface.asCtClass
            val metadata = supertype.rpcMetadata;
            val targetCls = createClass(iface.routerName, supertype) [
                addInterface(RpcImplementation.asCtClass)
                
                field(DELEGATE_FIELD, iface)
                //field(REMOTE_INVOKER_FIELD,iface);
                
                for (ctx : metadata.contexts) {
                    field(ctx.routingTableField, Map)
                }
                implementMethodsFrom(supertype) [
                    if (parameterTypes.size === 1) {
                        val rpcMeta = metadata.rpcMethods.get(name);
                        val bodyTmp = '''
                        {
                            final «InstanceIdentifier.name» identifier = $1.«rpcMeta.inputRouteGetter.name»()«IF rpcMeta.
                            routeEncapsulated».getValue()«ENDIF»;
                            «supertype.name» instance = («supertype.name») «rpcMeta.context.routingTableField».get(identifier);
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
                implementMethodsFrom(RpcImplementation.asCtClass) [
                    switch (name) {
                        case "getSupportedInputs":
                            body = '''
                            {
                                throw new java.lang.UnsupportedOperationException("Not implemented yet");
                                return ($r) null;
                            }'''
                        case "invoke": {
                            val tmpBody = '''
                            {
                                «FOR input : metadata.supportedInputs SEPARATOR " else "»
                                «val rpcMetadata = metadata.rpcInputs.get(input)»
                                if(«input.name».class.equals($1)) {
                                    return ($r) this.«rpcMetadata.methodName»((«input.name») $2);
                                }
                                «ENDFOR»
                                throw new java.lang.IllegalArgumentException("Not supported message type");
                                return ($r) null;
                            }
                            '''
                            body = tmpBody
                        }
                    }
                ]
            ]
            val instance = targetCls.toClass(iface.classLoader,iface.protectionDomain).newInstance as T
            return new RpcRouterCodegenInstance(iface, instance, metadata.contexts,metadata.supportedInputs);
        ];
        return instance;
    }

    private def RpcServiceMetadata getRpcMetadata(CtClass iface) {
        val metadata = new RpcServiceMetadata;
        
        iface.methods.filter[declaringClass == iface && parameterTypes.size === 1].forEach [ method |
            val routingPair = method.rpcMetadata;
            if (routingPair !== null) {
                metadata.contexts.add(routingPair.context)
                metadata.rpcMethods.put(method.name,routingPair)
                val input = routingPair.inputType.javaClass as Class<? extends DataContainer>;
                metadata.supportedInputs.add(input);
                metadata.rpcInputs.put(input,routingPair);
            }
        ]
        return metadata;
    }

    private def getRpcMetadata(CtMethod method) {
        val inputClass = method.parameterTypes.get(0); 
        return inputClass.rpcMethodMetadata(inputClass,method.name);
    }

    private def RpcMetadata rpcMethodMetadata(CtClass dataClass,CtClass inputClass,String rpcMethod) {
        for (method : dataClass.methods) {
            if (method.name.startsWith("get") && method.parameterTypes.size === 0) {
                for (annotation : method.availableAnnotations) {
                    if (annotation instanceof RoutingContext) {
                        val encapsulated = !method.returnType.equals(InstanceIdentifier.asCtClass);
                        return new RpcMetadata(null,rpcMethod,(annotation as RoutingContext).value, method, encapsulated,inputClass);
                    }
                }
            }
        }
        for (iface : dataClass.interfaces) {
            val ret = rpcMethodMetadata(iface,inputClass,rpcMethod);
            if(ret != null) return ret;
        }
        return null;
    }

    private def getJavaClass(CtClass cls) {
        Thread.currentThread.contextClassLoader.loadClass(cls.name)
    }

    override getInvokerFactory() {
        return this;
    }

    override invokerFor(NotificationListener instance) {
        val cls = instance.class
        val prototype = resolveInvokerClass(cls);

        return new RuntimeGeneratedInvoker(instance, prototype)
    }

    protected def generateListenerInvoker(Class<? extends NotificationListener> iface) {
        val callbacks = iface.methods.filter[notificationCallback]

        val supportedNotification = callbacks.map[parameterTypes.get(0) as Class<? extends Notification>].toSet;

        val targetCls = createClass(iface.invokerName, BROKER_NOTIFICATION_LISTENER) [
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
        val finalClass = targetCls.toClass(iface.classLoader, iface.protectionDomain)
        return new RuntimeGeneratedInvokerPrototype(supportedNotification,
            finalClass as Class<? extends org.opendaylight.controller.sal.binding.api.NotificationListener>);
    }

    private def void method(CtClass it, Class<?> returnType, String name, Class<?> parameter, MethodGenerator function1) {
        val method = new CtMethod(returnType.asCtClass, name, Arrays.asList(parameter.asCtClass), it);
        function1.process(method);
        it.addMethod(method);
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

    protected def resolveInvokerClass(Class<? extends NotificationListener> class1) {
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
package class RuntimeGeneratedInvoker implements NotificationInvoker {

    @Property
    val NotificationListener delegate;

    @Property
    var org.opendaylight.controller.sal.binding.api.NotificationListener invocationProxy;

    @Property
    var RuntimeGeneratedInvokerPrototype prototype;

    new(NotificationListener delegate, RuntimeGeneratedInvokerPrototype prototype) {
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
package class RuntimeGeneratedInvokerPrototype {

    @Property
    val Set<Class<? extends Notification>> supportedNotifications;

    @Property
    val Class<? extends org.opendaylight.controller.sal.binding.api.NotificationListener> protoClass;
}

package class RpcServiceMetadata {

    @Property
    val contexts = new HashSet<Class<? extends BaseIdentity>>();

    @Property
    val rpcMethods = new HashMap<String, RpcMetadata>();
    
    @Property
    val rpcInputs = new HashMap<Class<? extends DataContainer>, RpcMetadata>();
    
    
    @Property
    val supportedInputs = new HashSet<Class<? extends DataContainer>>();
}

@Data
package class RpcMetadata {

    @Property
    val QName qname;

    @Property
    val String methodName;

    @Property
    val Class<? extends BaseIdentity> context;
    @Property
    val CtMethod inputRouteGetter;

    @Property
    val boolean routeEncapsulated;
    
    @Property
    val CtClass inputType;
}
