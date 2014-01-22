package org.opendaylight.controller.sal.binding.dom.serializer.util;

import javassist.CtClass;

public interface ClassGenerator {
    void process(CtClass cls);
}
