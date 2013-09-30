package org.opendaylight.controller.sal.binding.codegen.impl

import java.lang.ClassLoader

class BrokerImplClassLoader extends ClassLoader {

    val ClassLoader spiClassLoader
    
    public new(ClassLoader model, ClassLoader spi) {
        super(model)
        spiClassLoader = spi;
    }

    override public loadClass(String name) throws ClassNotFoundException {
        try {
            return super.loadClass(name);
        } catch (ClassNotFoundException e) {
            return spiClassLoader.loadClass(name);
        }
    }

}
