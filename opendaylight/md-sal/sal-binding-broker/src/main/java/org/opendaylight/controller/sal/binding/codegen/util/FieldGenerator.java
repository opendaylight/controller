package org.opendaylight.controller.sal.binding.codegen.util;

import javassist.CtField;

public interface FieldGenerator {
    void process(CtField field);
}