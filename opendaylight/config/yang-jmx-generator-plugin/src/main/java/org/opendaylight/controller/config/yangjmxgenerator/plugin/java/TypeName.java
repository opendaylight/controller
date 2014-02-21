package org.opendaylight.controller.config.yangjmxgenerator.plugin.java;

public enum TypeName {

    classType("class"), interfaceType("interface"), enumType("enum"), absClassType("abstract class"), finalClassType("final class");

    private final String value;

    TypeName(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
