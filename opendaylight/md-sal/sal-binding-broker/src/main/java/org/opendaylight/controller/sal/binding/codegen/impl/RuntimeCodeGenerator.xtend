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

class RuntimeCodeGenerator {

    val ClassPool classPool;

    public new(ClassPool pool) {
        classPool = pool;
    }

    def <T extends RpcService> Class<? extends T> generateDirectProxy(Class<T> iface) {
        val supertype = iface.asCtClass
        val targetCls = createClass(iface.directProxyName, supertype) [
            field(DELEGATE_FIELD, iface);
            implementMethodsFrom(supertype) [
                body = '''return ($r) «DELEGATE_FIELD».«it.name»($$);'''
            ]
        ]
        return targetCls.toClass(iface.classLoader)
    }

    def <T extends RpcService> Class<? extends T> generateRouter(Class<T> iface) {
        val supertype = iface.asCtClass
        val targetCls = createClass(iface.routerName, supertype) [
            //field(ROUTING_TABLE_FIELD,Map)
            field(DELEGATE_FIELD, iface)
            val contexts = new HashMap<String, Class<? extends BaseIdentity>>();
            // We search for routing pairs and add fields
            supertype.methods.filter[declaringClass == supertype && parameterTypes.size === 1].forEach [ method |
                val routingPair = method.routingContextInput;
                if (routingPair !== null)
                    contexts.put(routingPair.context.routingTableField, routingPair.context);
            ]
            for (ctx : contexts.entrySet) {
                field(ctx.key, Map)
            }
            implementMethodsFrom(supertype) [
                if (parameterTypes.size === 1) {
                    val routingPair = routingContextInput;
                    val bodyTmp = '''
                    {
                        final «InstanceIdentifier.name» identifier = $1.«routingPair.getter.name»();
                        «supertype.name» instance = («supertype.name») «routingPair.context.routingTableField».get(identifier);
                        if(instance == null) {
                           instance = «DELEGATE_FIELD»;
                        }
                        return ($r) instance.«it.name»($$);
                    }'''
                    body = bodyTmp
                } else if (parameterTypes.size === 0) {
                    body = '''return ($r) «DELEGATE_FIELD».«it.name»($$);'''
                }
            ]
        ]
        return targetCls.toClass(iface.classLoader)
    }

    def Class<?> generateListenerInvoker(Class<? extends NotificationListener> iface) {
        val targetCls = createClass(iface.invokerName) [
            field(DELEGATE_FIELD, iface)
            it.method(Void, "invoke", Notification) [
                val callbacks = iface.methods.filter[notificationCallback]
                body = '''
                    {
                        «FOR callback : callbacks SEPARATOR " else "»
                            if($1 instanceof «val cls = callback.parameterTypes.get(0).name») {
                                «DELEGATE_FIELD».«callback.name»((«cls») $1);
                                return;
                            }
                        «ENDFOR»
                    }
                '''
            ]
        ]
        return targetCls.toClass(iface.classLoader);
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
            if (method.parameterTypes.size === 0 && method.name.startsWith("get")) {
                for (annotation : method.availableAnnotations) {
                    if (annotation instanceof RoutingContext) {
                        return new RoutingPair((annotation as RoutingContext).value, method)
                    }
                }
            }
        }
        for (iface : dataClass.interfaces) {
            val ret = getContextInstance(iface);
            if (ret != null) return ret;
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
            return pool.get(cls.name)
        }
    }
}
