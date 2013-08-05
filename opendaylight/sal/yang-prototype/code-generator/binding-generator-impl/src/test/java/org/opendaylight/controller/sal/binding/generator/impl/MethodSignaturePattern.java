package org.opendaylight.controller.sal.binding.generator.impl;

public class MethodSignaturePattern {
    final String name;
    final String type;

    public MethodSignaturePattern(String methodName, String methodType) {
        this.name = methodName;
        this.type = methodType;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }
}