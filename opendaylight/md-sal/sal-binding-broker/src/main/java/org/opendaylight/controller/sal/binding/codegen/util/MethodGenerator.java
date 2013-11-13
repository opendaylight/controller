package org.opendaylight.controller.sal.binding.codegen.util;

import javassist.CtMethod;

public interface MethodGenerator {
    void process(CtMethod method);
}