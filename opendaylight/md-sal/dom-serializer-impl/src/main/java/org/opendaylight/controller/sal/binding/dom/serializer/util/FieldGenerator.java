package org.opendaylight.controller.sal.binding.dom.serializer.util;

import javassist.CtField;

public interface FieldGenerator {
    void process(CtField field);
}
