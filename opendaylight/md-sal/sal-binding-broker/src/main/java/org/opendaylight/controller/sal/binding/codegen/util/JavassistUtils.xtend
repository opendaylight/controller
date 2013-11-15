package org.opendaylight.controller.sal.binding.codegen.util

import javassist.CtClass
import javassist.CtMethod
import javassist.ClassPool
import java.util.Arrays
import static com.google.common.base.Preconditions.*;
import javassist.CtField
import javassist.Modifier
import javassist.NotFoundException
import javassist.LoaderClassPath
import javassist.ClassClassPath
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class JavassistUtils {

    ClassPool classPool
    
    @Property
    val Lock lock = new ReentrantLock();

    new(ClassPool pool) {
        classPool = pool;
    }

    def void method(CtClass it, Class<?> returnType, String name, Class<?> parameter, MethodGenerator function1) {
        val method = new CtMethod(returnType.asCtClass, name, Arrays.asList(parameter.asCtClass), it);
        function1.process(method);
        it.addMethod(method);
    }
    
        def void method(CtClass it, Class<?> returnType, String name, Class<?> parameter1, Class<?> parameter2,  MethodGenerator function1) {
        val method = new CtMethod(returnType.asCtClass, name, Arrays.asList(parameter1.asCtClass,parameter2.asCtClass), it);
        function1.process(method);
        it.addMethod(method);
    }
    
    
    def void staticMethod(CtClass it, Class<?> returnType, String name, Class<?> parameter, MethodGenerator function1) {
        val method = new CtMethod(returnType.asCtClass, name, Arrays.asList(parameter.asCtClass), it);
        function1.process(method);
        it.addMethod(method);
    }

    def void implementMethodsFrom(CtClass target, CtClass source, MethodGenerator function1) {
        for (method : source.methods) {
            if (method.declaringClass == source) {
                val redeclaredMethod = new CtMethod(method, target, null);
                function1.process(redeclaredMethod);
                target.addMethod(redeclaredMethod);
            }
        }
    }

    def CtClass createClass(String fqn, ClassGenerator cls) {
        
        val target = classPool.makeClass(fqn);
        cls.process(target);
        return target;
    }

    def CtClass createClass(String fqn, CtClass superInterface, ClassGenerator cls) {
        
        val target = classPool.makeClass(fqn);
        target.implementsType(superInterface);
        cls.process(target);
        return target;
    }

    def void implementsType(CtClass it, CtClass supertype) {
        checkArgument(supertype.interface, "Supertype must be interface");
        addInterface(supertype);
    }

    def asCtClass(Class<?> class1) {
        classPool.get(class1);
    }

    def CtField field(CtClass it, String name, Class<?> returnValue) {
        val field = new CtField(returnValue.asCtClass, name, it);
        field.modifiers = Modifier.PUBLIC
        addField(field);
        return field;
    }
    
    def CtField staticField(CtClass it, String name, Class<?> returnValue) {
        val field = new CtField(returnValue.asCtClass, name, it);
        field.modifiers = Modifier.PUBLIC + Modifier.STATIC
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
}
