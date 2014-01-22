package org.opendaylight.controller.sal.binding.dom.serializer.util;

import javassist.CtMethod;

public interface MethodGenerator {
    void process(CtMethod method);
}
