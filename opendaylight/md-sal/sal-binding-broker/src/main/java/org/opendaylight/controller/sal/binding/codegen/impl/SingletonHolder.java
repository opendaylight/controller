package org.opendaylight.controller.sal.binding.codegen.impl;

import org.opendaylight.controller.sal.binding.codegen.RuntimeCodeGenerator;
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory;

import javassist.ClassPool;

public class SingletonHolder {

    public static final ClassPool CLASS_POOL = new ClassPool(); 
    public static final org.opendaylight.controller.sal.binding.codegen.impl.RuntimeCodeGenerator RPC_GENERATOR_IMPL = new org.opendaylight.controller.sal.binding.codegen.impl.RuntimeCodeGenerator(CLASS_POOL);
    public static final RuntimeCodeGenerator RPC_GENERATOR = RPC_GENERATOR_IMPL;
    public static final NotificationInvokerFactory INVOKER_FACTORY = RPC_GENERATOR_IMPL.getInvokerFactory();
}
