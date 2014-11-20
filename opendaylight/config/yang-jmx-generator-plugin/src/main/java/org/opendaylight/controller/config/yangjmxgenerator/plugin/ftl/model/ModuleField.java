/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.Dependency;

public class ModuleField extends Field {

    private final String nullableDefault, attributeName;
    private final boolean dependent, isListOfDependencies;
    private final Dependency dependency;

    private ModuleField(List<String> modifiers, String type, String name, String attributeName, String nullableDefault,
            boolean isDependency, Dependency dependency, boolean isListOfDependencies, boolean needsDepResolver) {
        super(modifiers, type, name, null, needsDepResolver);
        this.dependent = isDependency;
        this.dependency = dependency;
        this.attributeName = attributeName;
        if (type.startsWith(List.class.getName()) && nullableDefault == null) {
            String generics = type.substring(List.class.getName().length());
            nullableDefault = "new " + ArrayList.class.getName() + generics + "()";
        }
        this.nullableDefault = nullableDefault;
        this.isListOfDependencies = isListOfDependencies;
    }

    public ModuleField(String type, String name, String attributeName, String nullableDefault, boolean isDependency,
            Dependency dependency, boolean isListOfDependencies, boolean needsDepResolve) {
        this(Collections.<String> emptyList(), type, name, attributeName, nullableDefault, isDependency, dependency,
                isListOfDependencies, needsDepResolve);
    }

    public boolean isIdentityRef() {
        return false;
    }

    @Override
    public String toString() {
        return ModuleFieldSerializer.toString(this);
    }

    public Dependency getDependency() {
        return dependency;
    }

    public String getNullableDefault() {
        return nullableDefault;
    }

    public boolean isDependent() {
        return dependent;
    }

    public boolean isListOfDependencies() {
        return isListOfDependencies;
    }

    public String getAttributeName() {
        return attributeName;
    }


    public boolean isList() {
        return getType().startsWith("java.util.List");
    }

}
