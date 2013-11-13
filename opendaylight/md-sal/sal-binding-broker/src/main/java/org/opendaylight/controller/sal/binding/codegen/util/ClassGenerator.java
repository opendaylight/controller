package org.opendaylight.controller.sal.binding.codegen.util;

import javassist.CtClass;

public interface ClassGenerator {
    void process(CtClass cls);
}